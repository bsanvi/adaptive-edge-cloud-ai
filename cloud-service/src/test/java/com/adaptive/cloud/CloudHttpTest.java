package com.adaptive.cloud;

import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

class CloudHttpTest {
    static ConfigurableApplicationContext context;
    static String base;
    static final HttpClient client=HttpClient.newHttpClient();
    @BeforeAll static void start(){
        context=SpringApplication.run(CloudApplication.class,"--server.port=0","--server.address=127.0.0.1");
        base="http://127.0.0.1:"+((WebServerApplicationContext)context).getWebServer().getPort()+"/api/v1/cloud";
    }
    @AfterAll static void stop(){if(context!=null)context.close();}
    static HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    static HttpResponse<String> post(String path,String body)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
    @Test void healthAndReadiness()throws Exception{assertEquals(200,get("/health").statusCode());assertEquals(200,get("/ready").statusCode());}
    @Test void probeHasExactBytesAndBounds()throws Exception{
        var response=get("/probe?bytes=123");assertEquals(200,response.statusCode());assertEquals(123,response.body().length());
        assertEquals("no-store, no-transform",response.headers().firstValue("Cache-Control").orElseThrow());
        assertTrue(response.headers().firstValue("X-Server-Time").isPresent());
        assertEquals(400,get("/probe?bytes=0").statusCode());assertEquals(400,get("/probe?bytes=1048577").statusCode());
        assertEquals(400,get("/probe?bytes=abc").statusCode());
    }
    @Test void validPredictionAndPrivacyRejection()throws Exception{
        String body="{\"requestId\":\"r1\",\"taskId\":\"t1\",\"privacyLevel\":\"NORMAL\",\"samples\":[0,0,0,0,0,0,0,0]}";
        var prediction=post("/predict",body);assertEquals(200,prediction.statusCode());assertTrue(prediction.body().contains("NORMAL"));
        var denied=post("/predict",body.replace("NORMAL","CRITICAL"));assertEquals(403,denied.statusCode());assertTrue(denied.body().contains("r1"));
        assertEquals(400,post("/predict",body.replace("0,0,0,0,0,0,0,0","null,0,0,0,0,0,0,0")).statusCode());
        assertEquals(400,post("/predict",body.replace("NORMAL","UNKNOWN")).statusCode());
    }
    @Test void hybridSchemaMismatch()throws Exception{
        var response=post("/hybrid/infer","{\"requestId\":\"r1\",\"taskId\":\"t1\",\"privacyLevel\":\"NORMAL\",\"featureSchemaVersion\":\"wrong\",\"features\":[0,0,0,0,0,0,0,0]}");
        assertEquals(409,response.statusCode());
    }
    @Test void oversizedBodiesAndUnknownFieldsAreRejected()throws Exception{
        assertEquals(413,post("/predict"," ".repeat(1048577)).statusCode());
        String body="{\"requestId\":\"r1\",\"taskId\":\"t1\",\"privacyLevel\":\"NORMAL\",\"extra\":true,\"samples\":[0,0,0,0,0,0,0,0]}";
        assertEquals(400,post("/predict",body).statusCode());
        assertEquals(400,post("/predict",body.replace("\"extra\":true,","").replace("0,0,0,0,0,0,0,0","\"0\",0,0,0,0,0,0,0")).statusCode());
    }
}
