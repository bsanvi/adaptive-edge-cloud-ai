package com.team.aecai.orchestrator.research;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** On-demand REAL application-level network probe; no synthetic readings. */
@RestController
@Profile("integration")
@RequestMapping("/api/v1/research/network")
public class ResearchNetworkProbeController {
    private final String cloudUrl;
    public ResearchNetworkProbeController(@Value("${integration.cloud-url:http://127.0.0.1:8083}") String cloudUrl) {
        this.cloudUrl=cloudUrl.replaceAll("/+$", "");
    }
    @GetMapping("/live")
    public ResponseEntity<Map<String,Object>> sample(){
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("source","MEASURED_APPROXIMATE_APP_PROBE");
        result.put("sampledAt",Instant.now().toString());
        result.put("connected",false);
        result.put("latencyMs",null);
        result.put("bandwidthMbps",null);
        result.put("probeBytes",65536);
        HttpURLConnection c=null;
        try {
            c=(HttpURLConnection)URI.create(cloudUrl+"/api/v1/cloud/probe?bytes=65536").toURL().openConnection();
            c.setRequestMethod("GET");c.setConnectTimeout(1200);c.setReadTimeout(2200);
            c.setInstanceFollowRedirects(false);
            long started=System.nanoTime();
            if(c.getResponseCode()!=200)return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
            int len=0;
            try(InputStream in=c.getInputStream()){
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){len+=n;if(len>65536)break;}
            }
            double elapsed=(System.nanoTime()-started)/1_000_000.0;
            if(len!=65536 || !Double.isFinite(elapsed))return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
            // Effective payload throughput includes HTTP overhead and RTT; not link-layer speed.
            double bandwidth=65536*8/(1000*Math.max(.1,elapsed));
            result.put("connected",true);
            result.put("latencyMs",elapsed);
            result.put("bandwidthMbps",bandwidth);
            result.put("sampledAt",Instant.now().toString());
        } catch(Exception ignored) {
            // Cloud offline -> missing values, not invented measurements.
        } finally {if(c!=null)c.disconnect();}
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
}
