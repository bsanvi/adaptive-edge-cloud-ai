
package com.team.aecai.orchestrator.network;

import com.team.aecai.orchestrator.client.CloudApiClient;
import com.team.aecai.orchestrator.dto.CloudHealthResponse;
import com.team.aecai.orchestrator.dto.NetworkMetrics;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NetworkMonitorTest {

    @Test
    void shouldMeasureAvailableNetwork() {

        CloudApiClient cloud = mock(CloudApiClient.class);

        when(cloud.getHealth())
                .thenReturn(new CloudHealthResponse("online"));

        when(cloud.probe(256))
                .thenReturn(new byte[256 * 1024]);

        NetworkMonitor monitor =
                new NetworkMonitor(cloud, 256);

        NetworkMetrics result = monitor.measureNetwork();

        assertTrue(result.connected());
        assertTrue(result.latencyMs() >= 0);
        assertTrue(result.bandwidthMbps() > 0);

        verify(cloud).getHealth();
        verify(cloud).probe(256);
    }

    @Test
    void shouldHandleCloudUnavailable() {

        CloudApiClient cloud = mock(CloudApiClient.class);

        when(cloud.getHealth())
                .thenThrow(
                    new ResourceAccessException("Connection refused")
                );

        NetworkMonitor monitor =
                new NetworkMonitor(cloud, 256);

        NetworkMetrics result = monitor.measureNetwork();

        assertFalse(result.connected());
        assertEquals(0.0, result.bandwidthMbps());

        verify(cloud, never()).probe(256);
    }

    @Test
    void shouldRejectInvalidProbe() {

        CloudApiClient cloud = mock(CloudApiClient.class);

        when(cloud.getHealth())
                .thenReturn(new CloudHealthResponse("online"));

        when(cloud.probe(256))
                .thenReturn(new byte[100]);

        NetworkMonitor monitor =
                new NetworkMonitor(cloud, 256);

        NetworkMetrics result = monitor.measureNetwork();

        assertFalse(result.connected());
        assertEquals(0.0, result.bandwidthMbps());
    }

    @Test
    void shouldRejectOfflineStatus() {

        CloudApiClient cloud = mock(CloudApiClient.class);

        when(cloud.getHealth())
                .thenReturn(new CloudHealthResponse("offline"));

        NetworkMonitor monitor =
                new NetworkMonitor(cloud, 256);

        NetworkMetrics result = monitor.measureNetwork();

        assertFalse(result.connected());

        verify(cloud, never()).probe(256);
    }
}
