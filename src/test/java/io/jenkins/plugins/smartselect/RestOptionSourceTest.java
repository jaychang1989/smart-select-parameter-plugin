package io.jenkins.plugins.smartselect;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RestOptionSourceTest {
    private HttpServer server;
    private String origin;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            int status = 200;
            String body = "[{\"value\":\"price\",\"label\":\"报价服务\"}]";
            if (path.equals("/nested")) body = "{\"data\":{\"items\":" + body + "}}";
            if (path.equals("/bad")) body = "not-json";
            if (path.equals("/big")) body = "x".repeat(1_048_577);
            if (path.equals("/redirect")) {
                status = 302;
                exchange.getResponseHeaders().set("Location", origin + "/");
            }
            if (path.equals("/auth")
                    && !"Bearer secret-for-test"
                            .equals(exchange.getRequestHeaders().getFirst("Authorization"))) status = 401;
            if (path.equals("/slow"))
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    public void stop() {
        server.stop(0);
    }

    @Test
    public void readsLabelsFromArraysAndNestedObjects() {
        assertEquals(
                "报价服务",
                RestOptionSource.load(origin + "/", "", "", origin).get(0).getLabel());
        assertEquals(
                "price",
                RestOptionSource.load(origin + "/nested", "data.items", "", origin)
                        .get(0)
                        .getValue());
        assertEquals(
                "price",
                RestOptionSource.load(origin + "/auth", "", "secret-for-test", origin)
                        .get(0)
                        .getValue());
    }

    @Test
    public void rejectsUnapprovedOriginsBeforeAnyConnection() {
        for (String url :
                List.of(origin + "/", "file:///etc/passwd", origin.replace("http://", "http://user:pass@") + "/")) {
            assertThrows(
                    IllegalArgumentException.class, () -> RestOptionSource.load(url, "", "", "https://example.com"));
        }
        assertEquals(0, requests.get());
    }

    @Test
    public void rejectsRedirectsMalformedAndOversizedResponsesWithoutLeakingSecrets() {
        for (String path : List.of("/redirect", "/bad", "/big")) {
            IllegalArgumentException error = assertThrows(
                    IllegalArgumentException.class,
                    () -> RestOptionSource.load(origin + path, "", "secret-for-test", origin));
            assertFalse(error.getMessage().contains("secret-for-test"));
        }
        assertEquals(3, requests.get());
        assertThrows(
                IllegalArgumentException.class,
                () -> RestOptionSource.load(origin + "/nested", "missing.items", "", origin));
    }

    @Test
    public void slowResponseFailsWithinTheReadTimeout() {
        long start = System.nanoTime();
        assertThrows(IllegalArgumentException.class, () -> RestOptionSource.load(origin + "/slow", "", "", origin));
        long elapsed = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsed >= 2000 && elapsed < 8000, "Read timeout must be bounded: " + elapsed);
    }
}
