
package com.team.aecai.orchestrator.network;

import com.team.aecai.orchestrator.client.CloudApiClient;
import com.team.aecai.orchestrator.dto.CloudHealthResponse;
import com.team.aecai.orchestrator.dto.NetworkMetrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class NetworkMonitor {

    private final CloudApiClient cloudClient;
    private final int probeSizeKb;

    public NetworkMonitor(
            CloudApiClient cloudClient,
            @Value("${monitor.probe-size-kb:256}")
            int probeSizeKb) {

        this.cloudClient = cloudClient;
        this.probeSizeKb = probeSizeKb;
    }

    public NetworkMetrics measureNetwork() {

        try {
            // Measure health request round-trip time
            long latencyStart = System.nanoTime();

            CloudHealthResponse health =
                    cloudClient.getHealth();

            long latencyEnd = System.nanoTime();

            if (health == null ||
                    health.status() == null ||
                    !(health.status().equalsIgnoreCase("online")
                    || health.status().equalsIgnoreCase("ok"))) {

                return new NetworkMetrics(
                        false, 0.0, 0.0
                );
            }

            double latencyMs =
                    (latencyEnd - latencyStart)
                    / 1_000_000.0;

            // Measure HTTP probe transfer time
            long transferStart = System.nanoTime();

            byte[] data = cloudClient.probe(probeSizeKb);

            long transferEnd = System.nanoTime();

            double seconds =
                    (transferEnd - transferStart)
                    / 1_000_000_000.0;

            if (data == null ||
                    data.length != probeSizeKb * 1024 ||
                    seconds <= 0.0) {

                return new NetworkMetrics(
                        false, 0.0, 0.0
                );
            }

            double bandwidthMbps =
                    (data.length * 8.0)
                    / (seconds * 1_000_000.0);

            return new NetworkMetrics(
                    true,
                    latencyMs,
                    bandwidthMbps
            );

        } catch (RestClientException |
                 IllegalStateException ex) {

            // Conservatively reject remote execution
            // when a reliable probe is unavailable.
            return new NetworkMetrics(
                    false, 0.0, 0.0
            );
        }
    }
}
