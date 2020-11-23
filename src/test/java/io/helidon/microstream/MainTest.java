package io.helidon.microstream;

import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import java.util.concurrent.TimeUnit;

public class MainTest {

    private static WebServer webServer;
    private static WebClient webClient;

    @BeforeAll
    public static void startTheServer() throws Exception {
        webServer = Main.startServer();

        long timeout = 2000; // 2 seconds should be enough to start the server
        long now = System.currentTimeMillis();

        while (!webServer.isRunning()) {
            Thread.sleep(100);
            if ((System.currentTimeMillis() - now) > timeout) {
                Assertions.fail("Failed to start webserver");
            }
        }

        webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(JsonpSupport.create())
                .build();
    }

    @AfterAll
    public static void stopServer() throws Exception {
        if (webServer != null) {
            webServer.shutdown()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testMicrostreams() throws Exception {
        webClient.get()
                .path("/")
                .request(JsonObject.class)
                .thenAccept(jsonObject -> Assertions.assertEquals("Result: [Hello, World]!", jsonObject.getString("message")))
                .toCompletableFuture()
                .get();

        webClient.put()
                .path("/Helidon")
                .request(JsonObject.class)
                .thenAccept(jsonObject -> Assertions.assertEquals("Result: [Hello, World, Helidon]!", jsonObject.getString("message")))
                .toCompletableFuture()
                .get();

        webClient.delete()
                .path("/1")
                .request(JsonObject.class)
                .thenAccept(jsonObject -> Assertions.assertEquals("Result: [Hello, Helidon]!", jsonObject.getString("message")))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/1")
                .request(JsonObject.class)
                .thenAccept(jsonObject -> Assertions.assertEquals("Result: Helidon!", jsonObject.getString("message")))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/health")
                .request()
                .thenAccept(response -> Assertions.assertEquals(200, response.status().code()))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/metrics")
                .request()
                .thenAccept(response -> Assertions.assertEquals(200, response.status().code()))
                .toCompletableFuture()
                .get();
    }

}
