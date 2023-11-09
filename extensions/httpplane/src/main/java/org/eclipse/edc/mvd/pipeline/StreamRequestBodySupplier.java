package org.eclipse.edc.mvd.pipeline;

import java.io.InputStream;
import java.util.function.Supplier;

public class StreamRequestBodySupplier implements Supplier<InputStream> {
    private final InputStream stream;

    public StreamRequestBodySupplier(InputStream stream) {
        this.stream = stream;
    }

    @Override
    public InputStream get() {
        return stream;
    }
}
