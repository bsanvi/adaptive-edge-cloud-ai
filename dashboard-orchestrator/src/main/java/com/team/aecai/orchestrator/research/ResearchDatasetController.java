package com.team.aecai.orchestrator.research;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verified dataset registry with read-only, directory-confined image samples.
 * Empty repository = empty catalogue; no dummy entries or fabricated labels.
 */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/datasets")
public class ResearchDatasetController {
    private final JsonMapper mapper;
    private final Path root = Path.of("research-datasets").toAbsolutePath().normalize();
    public ResearchDatasetController(JsonMapper mapper) {this.mapper=mapper;}
    @GetMapping
    public List<Map<String,Object>> availableDatasets() {
        List<Map<String,Object>> result = new ArrayList<>();
        for(Map<String,Object> m : registry()) {
            Path directory = validatedDirectory(m);
            if(directory==null) continue;
            try {
                long loaded;
                try(var listing=Files.list(directory)) { loaded=listing.filter(ResearchDatasetController::imageFile).count(); }
                if(loaded==0) continue; // An empty directory is not a loaded dataset.
                Map<String,Object> entry=new LinkedHashMap<>();
                entry.put("id",String.valueOf(m.getOrDefault("id","unknown")));
                entry.put("name",String.valueOf(m.getOrDefault("name","Unknown")));
                entry.put("source",String.valueOf(m.getOrDefault("source","Undocumented")));
                entry.put("description",String.valueOf(m.getOrDefault("description","")));
                entry.put("taskType",String.valueOf(m.getOrDefault("taskType","image_classification")));
                entry.put("samples",loaded);
                entry.put("labels",m.get("labels") instanceof List<?> l?l:List.of());
                entry.put("license",String.valueOf(m.getOrDefault("license","Unknown")));
                entry.put("split",String.valueOf(m.getOrDefault("split","Undocumented")));
                entry.put("lastImported",String.valueOf(m.getOrDefault("lastImported","")));
                entry.put("modelCompatibility",String.valueOf(m.getOrDefault("modelCompatibility","Unknown")));
                entry.put("available",true);
                result.add(entry);
            } catch(IOException ignored){/* no unverified records */}
        }
        return result;
    }
    @GetMapping("/{id}/samples")
    public ResponseEntity<?> samples(@PathVariable String id){
        Path dir=lookup(id);
        if(dir==null)return ResponseEntity.notFound().build();
        try(var stream=Files.list(dir)){
            List<Map<String,Object>> items=new ArrayList<>();
            for(Path path:stream.filter(ResearchDatasetController::imageFile).sorted().limit(250).toList()){
                if(!path.toRealPath().startsWith(dir.toRealPath()))continue;
                Map<String,Object> data=new LinkedHashMap<>();
                data.put("fileName",path.getFileName().toString());
                data.put("sizeBytes",Files.size(path));
                data.put("previewUrl","/api/v1/research/datasets/"+id+"/samples/"+path.getFileName());
                data.put("label",label(id,path.getFileName().toString()));
                items.add(data);
            }
            return ResponseEntity.ok(items);
        }catch(Exception e){return ResponseEntity.internalServerError().body(Map.of("error","Dataset listing failed"));}
    }
    @GetMapping("/{id}/samples/{file}")
    public ResponseEntity<?> sample(@PathVariable String id,@PathVariable String file){
        Path dir=lookup(id);
        if(dir==null||file==null||!file.matches("[A-Za-z0-9_.-]{1,180}"))return ResponseEntity.notFound().build();
        Path image=dir.resolve(file).normalize();
        try{
            if(!imageFile(image)||!image.toRealPath().startsWith(dir.toRealPath())||Files.size(image)>10_485_760)
                return ResponseEntity.notFound().build();
            String ext=file.toLowerCase();
            MediaType type=ext.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG;
            return ResponseEntity.ok().contentType(type).header("X-Content-Type-Options","nosniff")
                    .body(new FileSystemResource(image));
        }catch(Exception e){return ResponseEntity.notFound().build();}
    }
    private List<Map<String,Object>> registry(){
        Path path=root.resolve("registry.json");
        if(!Files.isRegularFile(path))return List.of();
        try{
            if(Files.size(path)>200_000)return List.of();
            @SuppressWarnings("unchecked") List<Map<String,Object>> result=mapper.readValue(Files.readString(path),List.class);
            return result==null?List.of():result;
        }catch(Exception e){return List.of();}
    }
    private Path lookup(String id){
        if(id==null||!id.matches("[A-Za-z0-9_.-]{1,80}"))return null;
        for(Map<String,Object> meta:registry())if(id.equals(meta.get("id")))return validatedDirectory(meta);
        return null;
    }
    private Path validatedDirectory(Map<String,Object> meta){
        Object relative=meta.get("directory");
        if(!(relative instanceof String path)||path.isBlank()||path.contains("..")||path.startsWith("/")||path.contains(":")||path.contains("\\"))return null;
        Path dir=root.resolve(path).normalize();
        try{return dir.startsWith(root)&&Files.isDirectory(dir)&&dir.toRealPath().startsWith(root.toRealPath())?dir:null;}
        catch(IOException e){return null;}
    }
    private String label(String id,String file){
        // Labels must be explicitly supplied by maintainers in registry entry's sampleLabels.
        for(Map<String,Object> m:registry()){
            if(!id.equals(m.get("id")))continue;
            if(m.get("sampleLabels") instanceof Map<?,?> labels && labels.get(file) instanceof String s)return s;
        }
        return "UNLABELED";
    }
    private static boolean imageFile(Path f){
        String name=f.getFileName().toString().toLowerCase();
        return Files.isRegularFile(f)&&(name.endsWith(".png")||name.endsWith(".jpg")||name.endsWith(".jpeg"));
    }
}
