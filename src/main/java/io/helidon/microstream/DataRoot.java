package io.helidon.microstream;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry Alexandrov on 6.11.20.
 */
public class DataRoot {
    private final List<String> items = new ArrayList<>();

    public DataRoot() {
        super();
    }

    public List<String> items() {
        return this.items;
    }
}
