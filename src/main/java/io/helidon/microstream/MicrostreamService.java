package io.helidon.microstream;

import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import one.microstream.memory.android.MicroStreamAndroidAdapter;
import one.microstream.storage.types.EmbeddedStorage;
import one.microstream.storage.types.EmbeddedStorageManager;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Dmitry Alexandrov on 6.11.20.
 */
public class MicrostreamService implements Service {
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
    private static final Logger LOGGER = Logger.getLogger(MicrostreamService.class.getName());

    static {
        //trick for native image
        MicroStreamAndroidAdapter.setupFull();
    }

    private final EmbeddedStorageManager storageManager;

    public MicrostreamService() {
        this.storageManager = EmbeddedStorage.start(new DataRoot(), Paths.get("data"));
        ((DataRoot) storageManager.root()).items().clear();
        ((DataRoot) storageManager.root()).items().add("Hello");
        ((DataRoot) storageManager.root()).items().add("World");

    }

    private static <T> T processErrors(Throwable ex, ServerRequest request, ServerResponse response) {

        if (ex.getCause() instanceof JsonException) {

            LOGGER.log(Level.FINE, "Invalid JSON", ex);
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "Invalid JSON")
                    .build();
            response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
        } else {

            LOGGER.log(Level.FINE, "Internal error", ex);
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "Internal error")
                    .build();
            response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(jsonErrorObject);
        }

        return null;
    }

    private DataRoot root() {
        return (DataRoot) this.storageManager.root();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/", this::getAllItems)
                .get("/{index}", this::getItem)
                .put("/{text}", this::addNewItem)
                .delete("/{index}", this::deleteItemHandler);
    }

    private void getAllItems(ServerRequest request, ServerResponse response) {
        List<String> items = root().items();
        sendResponse(response, Arrays.toString(items.toArray()));
    }

    private void getItem(ServerRequest request, ServerResponse response) {
        String index = request.path().param("index");
        sendResponse(response, root().items().get(Integer.parseInt(index)));
    }

    private void addNewItem(ServerRequest request, ServerResponse response) {
        String text = request.path().param("text");
        root().items().add(text);
        sendResponse(response, Arrays.toString(root().items().toArray()));
    }

    private void sendResponse(ServerResponse response, String item) {
        String msg = String.format("Result: %s!", item);

        JsonObject returnObject = JSON.createObjectBuilder()
                .add("message", msg)
                .build();
        response.send(returnObject);
    }

    private void deleteItemHandler(ServerRequest request,
                                   ServerResponse response) {
        int index = Integer.parseInt(request.path().param("index"));
        final List<String> items = this.root().items();
        if (index < 0 || index >= items.size()) {
            response.status(Http.Status.BAD_REQUEST_400)
                    .send();
        } else {
            items.remove(index);
            this.storageManager.store(items);
            sendResponse(response, Arrays.toString(root().items().toArray()));
        }
    }
}
