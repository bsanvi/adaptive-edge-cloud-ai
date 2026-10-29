package com.team.aecai.orchestrator.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only backend checks; front-end never assumes another port is proxied by 8080. */
@Service
@Profile("integration")
public class IntegrationServiceStatus {
    private final String edgeUrl, cloudUrl, estimatorUrl, decisionUrl;
    public IntegrationServiceStatus(
            @Value("${integration.edge-url:http://127.0.0.1:8081}") String edgeUrl,
            @Value("${integration.cloud-url:http://127.0.0.1:8083}") String cloudUrl,
            @Value("${integration.estimator-url:http://127.0.0.1:8084}") String estimatorUrl,
            @Value("${integration.decision-url:http://127.0.0.1:8001}") String decisionUrl) {
        this.edgeUrl=edgeUrl; this.cloudUrl=cloudUrl;
        this.estimatorUrl=estimatorUrl; this.decisionUrl=decisionUrl;
    }
    private boolean available(String base, String path) {
        HttpURLConnection conn=null;
        try {
            conn=(HttpURLConnection)URI.create(base+path).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(900);
            conn.setReadTimeout(1200);
            conn.setInstanceFollowRedirects(false);
            return conn.getResponseCode()==200;
        } catch(Exception e) {return false;}
        finally {if(conn!=null)conn.disconnect();}
    }
    public Map<String,Boolean> statuses(){
        Map<String,Boolean> r=new LinkedHashMap<>();
        r.put("Dashboard",true);
        r.put("Edge", available(edgeUrl,"/api/v1/edge/anomaly/ready"));
        r.put("Decision",available(decisionUrl,"/health"));
        r.put("Cloud",available(cloudUrl,"/api/v1/cloud/ready"));
        r.put("Estimator",available(estimatorUrl,"/api/v1/estimate/health"));
        return r;
    }
}
