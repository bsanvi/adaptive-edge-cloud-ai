package com.team.aecai.orchestrator.research;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Authenticated dataset import with explicit provenance. No synthetic labels,
 * filesystem path input or arbitrary browser-supplied file names are used.
 */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/datasets/admin")
public class ResearchDatasetAdminController {
    private static final long MAX_FILE=10_485_760;
    private static final long MAX_PIXELS=16_000_000;
    private final Path root=Path.of("research-datasets").toAbsolutePath().normalize();
    private final JsonMapper mapper;
    private final ResearchOperationsAuth auth;
    public ResearchDatasetAdminController(JsonMapper mapper,ResearchOperationsAuth auth){
        this.mapper=mapper;this.auth=auth;
    }

    static boolean validId(String id){return id!=null&&id.matches("[A-Za-z0-9_-]{1,64}");}
    static boolean validMeta(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max;}

    @PostMapping(path="/import",consumes="multipart/form-data")
    public synchronized ResponseEntity<?> importImage(HttpServletRequest request,
            @RequestParam("image") MultipartFile file,
            @RequestParam String datasetId,@RequestParam String name,
            @RequestParam String source,@RequestParam String license,
            @RequestParam(defaultValue="train") String split,
            @RequestParam(defaultValue="UNLABELED") String label) {
        auth.require(request);
        if(!validId(datasetId) || !validMeta(name,100) || !validMeta(source,300)
                || !validMeta(license,200) || !validMeta(split,40) || !validMeta(label,120)
                || file==null || file.isEmpty() || file.getSize()>MAX_FILE)
            return ResponseEntity.badRequest().body(Map.of("error","Invalid import metadata or image (max 10 MiB)"));
        Path stored=null;
        try {
            byte[] bytes=file.getBytes();
            if(bytes.length==0 || bytes.length>MAX_FILE)return ResponseEntity.badRequest().body(Map.of("error","Image size invalid"));
            BufferedImage image=readImage(bytes);
            if(image==null)return ResponseEntity.badRequest().body(Map.of("error","Not a supported PNG/JPEG image or too many pixels"));
            Files.createDirectories(root);
            if(Files.isSymbolicLink(root))return ResponseEntity.badRequest().body(Map.of("error","Invalid dataset root"));
            Path dir=root.resolve(datasetId).normalize();
            if(!dir.startsWith(root)||Files.isSymbolicLink(dir))return ResponseEntity.badRequest().body(Map.of("error","Invalid dataset path"));
            Files.createDirectories(dir);
            List<Map<String,Object>> registry=readRegistry();
            Map<String,Object> entry=registry.stream().filter(x->datasetId.equals(x.get("id"))).findFirst().orElse(null);
            if(entry!=null && (!name.equals(entry.get("name")) || !source.equals(entry.get("source"))
                    || !license.equals(entry.get("license"))))
                return ResponseEntity.badRequest().body(Map.of("error","Existing dataset provenance differs; use a new ID"));
            String fileName=UUID.randomUUID()+".png";
            stored=dir.resolve(fileName);
            if(!ImageIO.write(image,"png",stored.toFile()))throw new IOException("PNG writer unavailable");
            if(entry==null){
                entry=new LinkedHashMap<>();entry.put("id",datasetId);entry.put("name",name);entry.put("source",source);
                entry.put("license",license);entry.put("directory",datasetId);
                entry.put("taskType","image_classification");
                entry.put("description","Uploaded research dataset; provenance supplied by importer");
                entry.put("modelCompatibility","EDGE_IMAGE_SQUEEZENET_ONLY");
                entry.put("labels",new ArrayList<String>());
                entry.put("sampleLabels",new LinkedHashMap<String,String>());
                registry.add(entry);
            }
            entry.put("split",split);entry.put("lastImported",Instant.now().toString());
            @SuppressWarnings("unchecked") Map<String,String> labels=entry.get("sampleLabels") instanceof Map<?,?> old?
                    new LinkedHashMap<>((Map<String,String>)old):new LinkedHashMap<>();
            labels.put(fileName,label);entry.put("sampleLabels",labels);
            @SuppressWarnings("unchecked") List<String> categories=entry.get("labels") instanceof List<?> old?
                    new ArrayList<>((List<String>)old):new ArrayList<>();
            if(!"UNLABELED".equals(label)&&!categories.contains(label))categories.add(label);
            entry.put("labels",categories);
            writeRegistry(registry);
            return ResponseEntity.ok(Map.of("datasetId",datasetId,"fileName",fileName,"label",label,
                    "width",image.getWidth(),"height",image.getHeight(),"sizeBytes",Files.size(stored),
                    "source","REAL_USER_UPLOAD","importedAt",entry.get("lastImported")));
        }catch(Exception ex){
            if(stored!=null){try{Files.deleteIfExists(stored);}catch(IOException ignored){}}
            return ResponseEntity.internalServerError().body(Map.of("error","Dataset import could not be persisted"));
        }
    }

    private static BufferedImage readImage(byte[] bytes) throws IOException {
        try(ImageInputStream stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            if(stream==null)return null;
            Iterator<ImageReader> readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext())return null;
            ImageReader reader=readers.next();
            try{
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!format.equals("png")&&!format.equals("jpeg")&&!format.equals("jpg"))return null;
                reader.setInput(stream,true,true);
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<=0||height<=0||(long)width*height>MAX_PIXELS)return null;
                return reader.read(0);
            }finally{reader.dispose();}
        }
    }

    private List<Map<String,Object>> readRegistry() throws IOException {
        Path file=root.resolve("registry.json");
        if(!Files.isRegularFile(file))return new ArrayList<>();
        if(Files.size(file)>200_000)throw new IOException("Registry too large");
        @SuppressWarnings("unchecked") List<Map<String,Object>> data=mapper.readValue(Files.readString(file),List.class);
        if(data==null)throw new IOException("Invalid registry");
        return new ArrayList<>(data);
    }
    private void writeRegistry(List<Map<String,Object>> registry) throws IOException {
        String data=mapper.writeValueAsString(registry);
        if(data.length()>200_000)throw new IOException("Registry limit reached");
        Path file=root.resolve("registry.json");
        if(Files.isRegularFile(file))Files.copy(file,root.resolve("registry.json.backup"),StandardCopyOption.REPLACE_EXISTING);
        Path temp=Files.createTempFile(root,"registry-",".tmp");
        try{
            Files.writeString(temp,data,StandardCharsets.UTF_8);
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
}
