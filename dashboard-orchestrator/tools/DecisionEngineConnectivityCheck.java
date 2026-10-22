
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DecisionEngineConnectivityCheck {

    public static void main(String[] args) {

        String baseUrl = "http://127.0.0.1:8001";

        // Sample task matching Person 2's Python API contract.
        String json = """
            {
              "task": {
                "task_id": "java-test-001",
                "task_type": "anomaly_detection",
                "data_size_mb": 1.0,
                "complexity": 0.5,
                "deadline_ms": 1000.0,
                "privacy_required": true
              },
              "edge": {
                "cpu_percent": 30.0,
                "ram_percent": 40.0,
                "battery_percent": 80.0,
                "power_plugged": true
              },
              "network": {
                "latency_ms": 20.0,
                "bandwidth_mbps": 50.0
              },
              "estimates": {
                "edge": {
                  "estimated_time_ms": 100.0,
                  "estimated_energy_j": 5.0,
                  "estimated_cost": 0.0,
                  "estimated_carbon_g": 0.1
                },
                "cloud": null,
                "hybrid": null
              },
              "previous_location": null
            }
            """;

        System.out.println("====================================");
        System.out.println(" JAVA TO PYTHON CONNECTIVITY TEST");
        System.out.println("====================================");

        try (HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10))
        .build()) {

            // STEP 1: Check the Python server's health.
            System.out.println("\nSTEP 1: Checking Python server...");

            HttpRequest healthRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> healthResponse = client.send(
                    healthRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(
                    "Health status: " + healthResponse.statusCode()
            );
            System.out.println(
                    "Health response: " + healthResponse.body()
            );

            if (healthResponse.statusCode() != 200) {
                throw new IllegalStateException(
                        "Python Decision Engine is not healthy"
                );
            }

            // STEP 2: Prepare the JSON request.
            System.out.println("\nSTEP 2: Preparing JSON request...");

            System.out.println("JSON being sent:");
            System.out.println(json);
            System.out.println("JSON length: " + json.length());

            if (json.isBlank()) {
                throw new IllegalStateException(
                        "JSON request body is empty"
                );
            }

            // STEP 3: Build HTTP POST request.
            System.out.println("\nSTEP 3: Sending request to Python...");

            HttpRequest decisionRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/decision/evaluate"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            System.out.println(
                    "Request method: " + decisionRequest.method()
            );
            System.out.println(
                    "Request URL: " + decisionRequest.uri()
            );

            // STEP 4: Send request and collect response.
            HttpResponse<String> decisionResponse = client.send(
                    decisionRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("\nSTEP 4: Python response received!");

            System.out.println(
                    "Decision status: " + decisionResponse.statusCode()
            );
            System.out.println(
                    "Decision response: " + decisionResponse.body()
            );

            // STEP 5: Validate HTTP response.
            if (decisionResponse.statusCode() != 200) {
                throw new IllegalStateException(
                        "Decision request failed with HTTP status "
                        + decisionResponse.statusCode()
                );
            }

            // STEP 6: Display success.
            System.out.println("\n====================================");
            System.out.println(" INTEGRATION TEST SUCCESSFUL");
            System.out.println("====================================");

            System.out.println(
                    "Java successfully communicated with Python!"
            );

            System.out.println(
                    "Person 2 Decision Engine responded successfully."
            );

        } catch (Exception exception) {

            System.err.println("\nINTEGRATION TEST FAILED");
            System.err.println(
                    "Error: " + exception.getMessage()
            );

            exception.printStackTrace();
            System.exit(1);
        }
    }
}
