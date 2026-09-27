package com.adaptive.estimate;

import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

class EstimateHttpTest {
    static ConfigurableApplicationContext context;
    static String base;
    static final HttpClient client=HttpClient.newHttpClient();
    @BeforeAll static void start(){context=SpringApplication.run(EstimatorApplication.class,"--server.port=0");
        base="http://127.0.0.1:"+((WebServerApplicationContext)context).getWebServer().getPort()+"/api/v1/estimate";}
    @AfterAll static void stop(){if(context!=null)context.close();}
    static HttpResponse<String> post(String body)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(base+"/routes")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
    static String offline(){return """
        {"requestId":"r1","task":{"taskId":"t1","taskType":"SENSOR_ANOMALY","complexity":"LOW",
        "dataSizeBytes":100,"estimatedFeatureBytes":90,"estimatedResultBytes":80},
        "device":{"cpuPercent":20,"ramPercent":30,"batteryPercent":null,"source":"MEASURED"},
        "network":{"connected":false,"bandwidthMbps":null,"networkLatencyMs":null,"sampledAt":null,"source":"MEASURED"},
        "edgeCapable":true,"cloudCapable":true,"remainingDeadlineMs":1000,
        "edgeCarbonIntensityGPerKwh":500,"cloudCarbonIntensityGPerKwh":500}
        """;}
    @Test void offlineAndInvalidContexts()throws Exception{
        var response=post(offline());assertEquals(200,response.statusCode());assertTrue(response.body().contains("NETWORK_UNAVAILABLE"));
        assertEquals(400,post(offline().replace("\"dataSizeBytes\":100","\"dataSizeBytes\":2000000")).statusCode());
        assertEquals(400,post(offline().replace("\"cpuPercent\":20","\"cpuPercent\":101")).statusCode());
        assertEquals(400,post(offline().replace("\"LOW\"","\"INVALID\"")).statusCode());
        assertEquals(400,post(offline().replace("\"edgeCapable\":true,","")).statusCode());
    }
}
