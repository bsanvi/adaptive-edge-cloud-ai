package com.edgecloud.edge.research;

import com.edgecloud.edge.dto.TaskProfileRequest;
import com.edgecloud.edge.service.TaskProfilerService;
import com.edgecloud.edge.service.MlInferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Additive bridge for Person 1's actual TaskProfilerService and image model.
 * Install ONLY when Person 1's image profiling and SqueezeNet components exist.
 * Never accepts an arbitrary browser-provided local filesystem path.
 */
@RestController
@RequestMapping("/api/v1/edge/image")
public class SecureImageBridgeController {
    private static final long MAX_BYTES=10L*1024*1024;
    private static final long MAX_PIXELS=16_000_000L;
    private static final long TTL_MILLIS=15L*60_000;
    private final TaskProfilerService profiler;
    private final MlInferenceService ml;
    private final Path directory;
    private final ConcurrentMap<String,Stored> store=new ConcurrentHashMap<>();
    private record Stored(Path path,long timestamp,String taskId){}
    public SecureImageBridgeController(TaskProfilerService profiler,MlInferenceService ml) throws Exception {
        this.profiler=profiler;this.ml=ml;
        directory=Path.of(System.getProperty("java.io.tmpdir"),"edgecloud-image-staging").toAbsolutePath().normalize();
        Files.createDirectories(directory);
    }
    @PostMapping("/upload")
    public ResponseEntity<?> upload(HttpServletRequest request, @RequestParam("image") MultipartFile image,
            @RequestParam("taskId") String taskId,@RequestParam("deadlineMs") double deadlineMs,
            @RequestParam("privacyLevel") String privacyLevel,
            @RequestParam("priorityProfile") String priorityProfile) {
        if(!isLoopback(request))return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Edge image staging is local-only; use Dashboard gateway"));
        sweep();
        if(taskId==null||!taskId.matches("[A-Za-z0-9_.-]{1,100}")||!Double.isFinite(deadlineMs)||deadlineMs<=0||deadlineMs>10_000_000)
            return ResponseEntity.badRequest().body(Map.of("error","Invalid task identifier or deadline"));
        if(!java.util.Set.of("PRIVATE","NORMAL").contains(privacyLevel)||!java.util.Set.of("BALANCED","PERFORMANCE","SUSTAINABILITY").contains(priorityProfile))
            return ResponseEntity.badRequest().body(Map.of("error","Invalid privacy or priority"));
        if(image==null||image.isEmpty()||image.getSize()>MAX_BYTES)
            return ResponseEntity.badRequest().body(Map.of("error","Image must be non-empty and no larger than 10MB"));
        Path temp=null;
        try {
            byte[] bytes=image.getBytes();
            String suffix;
            if(bytes.length>=3&&(bytes[0]&255)==0xff&&(bytes[1]&255)==0xd8&&(bytes[2]&255)==0xff)suffix=".jpg";
            else if(bytes.length>=8&&(bytes[0]&255)==137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71)suffix=".png";
            else return ResponseEntity.badRequest().body(Map.of("error","Only genuine PNG/JPEG images are supported"));
            temp=Files.createTempFile(directory,"image-",suffix);
            Files.write(temp,bytes,StandardOpenOption.TRUNCATE_EXISTING);
            try(ImageInputStream imageInput=ImageIO.createImageInputStream(temp.toFile())) {
                Iterator<ImageReader> readers=ImageIO.getImageReaders(imageInput);
                if(!readers.hasNext())throw new IllegalArgumentException("Unsupported image format");
                ImageReader reader=readers.next();
                try {reader.setInput(imageInput,true,true);long width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<=0||height<=0||width*height>MAX_PIXELS)throw new IllegalArgumentException("Image resolution exceeds 16M pixels");
                }finally {reader.dispose();}
            }
            // Reuse Person 1's actual profiler and its complexity rules.
            var profile=profiler.profileTask(new TaskProfileRequest(taskId,temp.toString(),deadlineMs,privacyLevel,priorityProfile));
            String token=UUID.randomUUID().toString();
            store.put(token,new Stored(temp,System.currentTimeMillis(),taskId));
            return ResponseEntity.ok(Map.of(
                    "ref",token,"taskId",taskId,"fileName",image.getOriginalFilename()==null?"image":Path.of(image.getOriginalFilename()).getFileName().toString(),
                    "fileSizeBytes",bytes.length,"profilingTimestamp",Instant.now().toString(),
                    "profileSource","PERSON1_EDGE_TASK_PROFILER",
                    "profile",Map.of("task_id",profile.task_id(),"task_type",profile.task_type(),
                        "input_data_size_bytes",profile.input_data_size_bytes(),"image_width",profile.image_width(),
                        "image_height",profile.image_height(),"total_pixels",profile.total_pixels(),
                        "complexity",profile.complexity(),"status",profile.status())));
        } catch(Exception e) {
            if(temp!=null)try {Files.deleteIfExists(temp);}catch(Exception ignored){}
            return ResponseEntity.badRequest().body(Map.of("error","Failed to validate or profile image"));
        }
    }
    @PostMapping("/classify/{ref}")
    public ResponseEntity<?> classify(HttpServletRequest request, @PathVariable String ref){
        if(!isLoopback(request))return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Edge classifier is local-only; use Dashboard gateway"));
        sweep();if(ref==null||!ref.matches("[a-fA-F0-9-]{36}"))return ResponseEntity.badRequest().body(Map.of("error","Invalid image reference"));
        Stored stored=store.get(ref);
        if(stored==null||System.currentTimeMillis()-stored.timestamp()>TTL_MILLIS||!Files.isRegularFile(stored.path()))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Image reference expired or unavailable"));
        try {
            var prediction=ml.classifyImage(stored.path().toString());
            return ResponseEntity.ok(Map.of("predicted_label",prediction.predicted_label(),
                    "predicted_class_index",prediction.predicted_class_index(),
                    "confidence",prediction.confidence(),"execution_time_ms",prediction.execution_time_ms(),
                    "status",prediction.status(),"taskId",stored.taskId(),"execution_route","EDGE","source","PERSON1_SQUEEZENET_MODEL"));
        }catch(Exception e){return ResponseEntity.status(502).body(Map.of("error","Edge image classification failed"));}
    }
    private static boolean isLoopback(HttpServletRequest request){
        try{return InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();}
        catch(Exception ex){return false;}
    }
    private void sweep(){long now=System.currentTimeMillis();store.forEach((key,stored)->{if(now-stored.timestamp()>TTL_MILLIS){if(store.remove(key,stored))try{Files.deleteIfExists(stored.path());}catch(Exception ignored){}}});}
}
