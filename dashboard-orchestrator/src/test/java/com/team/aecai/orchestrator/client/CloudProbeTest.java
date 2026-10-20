
package com.team.aecai.orchestrator.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudProbeTest {

    @Test
    void shouldDownloadProbePayload() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        CloudApiClient client = new CloudApiClient(
                builder,
                "http://localhost:8083"
        );

        byte[] payload = new byte[256 * 1024];

        server.expect(requestTo(
                "http://localhost:8083/api/v1/cloud/probe?sizeKb=256"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                payload,
                MediaType.APPLICATION_OCTET_STREAM
            ));

        byte[] result = client.probe(256);

        assertNotNull(result);
        assertEquals(256 * 1024, result.length);

        server.verify();
    }
}
