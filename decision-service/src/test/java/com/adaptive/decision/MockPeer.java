package com.adaptive.decision;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Local synthetic peer; no real Edge, Cloud or Orchestrator process is contacted. */
final class MockPeer implements AutoCloseable {
    final HttpServer server;
    final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    final AtomicInteger calls = new AtomicInteger();
    final AtomicReference<String> body = new AtomicReference<>();
    volatile int status = 200;
    volatile String response = "{}";
    volatile long delayMs;
    MockPeer(String path) {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(executor);
            server.createContext(path, exchange -> {
                calls.incrementAndGet();
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                int replyStatus = status;
                String replyBody = response;
                try {
                    if (delayMs > 0) Thread.sleep(delayMs);
                    byte[] bytes = replyBody.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(replyStatus, replyStatus == 204 ? -1 : bytes.length);
                    if (replyStatus != 204) exchange.getResponseBody().write(bytes);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                catch (java.io.IOException disconnectedClient) { /* Expected for timeout tests. */ }
                finally { exchange.close(); }
            });
            server.start();
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
    String url() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); executor.shutdownNow(); }
}
