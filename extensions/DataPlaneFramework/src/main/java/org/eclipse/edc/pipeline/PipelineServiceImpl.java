/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.pipeline;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Default pipeline service implementation.
 */
public class PipelineServiceImpl implements PipelineService {
    private final List<DataSourceFactory> sourceFactories = new ArrayList<>();
    private final List<DataSinkFactory> sinkFactories = new ArrayList<>();
    private final Monitor monitor;

    public static Instant transferStartTime;

    public PipelineServiceImpl(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return getSourceFactory(request) != null &&
                getSinkFactory(request) != null;
    }

    @Override
    public Result<Boolean> validate(DataFlowRequest request) {
        var sourceFactory = getSourceFactory(request);
        if (sourceFactory == null) {
            // NB: do not include the source type as that can possibly leak internal information
            return Result.failure("Data source not supported for: " + request.getId());
        }

        var sourceValidation = sourceFactory.validateRequest(request);
        if (sourceValidation.failed()) {
            return Result.failure(sourceValidation.getFailureMessages());
        }

        var sinkFactory = getSinkFactory(request);
        if (sinkFactory == null) {
            // NB: do not include the target type as that can possibly leak internal information
            return Result.failure("Data sink not supported for: " + request.getId());
        }

        var sinkValidation = sinkFactory.validateRequest(request);
        if (sinkValidation.failed()) {
            return Result.failure(sinkValidation.getFailureMessages());
        }

        return Result.success(true);
    }

    @WithSpan
    @Override
    public CompletableFuture<StreamResult<Void>> transfer(DataFlowRequest request) {
        transferStartTime = Instant.now();
        var sourceFactory = getSourceFactory(request);
        if (sourceFactory == null) {
            return noSourceFactory(request);
        }
        var sinkFactory = getSinkFactory(request);
        if (sinkFactory == null) {
            return noSinkFactory(request);
        }
        var source = sourceFactory.createSource(request);
        var sink = sinkFactory.createSink(request);
        monitor.debug(() -> format("Transferring from %s to %s.", request.getSourceDataAddress().getType(), request.getDestinationDataAddress().getType()));
        return sink.transfer(source);
    }

    @Override
    public CompletableFuture<StreamResult<Void>> transfer(DataSource source, DataFlowRequest request) {
        var sinkFactory = getSinkFactory(request);
        if (sinkFactory == null) {
            return noSinkFactory(request);
        }
        var sink = sinkFactory.createSink(request);
        monitor.debug(() -> format("Transferring from %s to %s.", request.getSourceDataAddress().getType(), request.getDestinationDataAddress().getType()));
        return sink.transfer(source);
    }

    @Override
    public CompletableFuture<StreamResult<Void>> transfer(DataSink sink, DataFlowRequest request) {
        var sourceFactory = getSourceFactory(request);
        if (sourceFactory == null) {
            return noSourceFactory(request);
        }
        var source = sourceFactory.createSource(request);
        monitor.debug(() -> format("Transferring from %s to %s.", request.getSourceDataAddress().getType(), request.getDestinationDataAddress().getType()));
        return sink.transfer(source);
    }

    @Override
    public void registerFactory(DataSourceFactory factory) {
        sourceFactories.add(factory);
    }

    @Override
    public void registerFactory(DataSinkFactory factory) {
        sinkFactories.add(factory);
    }

    @Nullable
    private DataSourceFactory getSourceFactory(DataFlowRequest request) {
        return sourceFactories.stream().filter(s -> s.canHandle(request)).findFirst().orElse(null);
    }

    @Nullable
    private DataSinkFactory getSinkFactory(DataFlowRequest request) {
        return sinkFactories.stream().filter(s -> s.canHandle(request)).findFirst().orElse(null);
    }

    @NotNull
    private CompletableFuture<StreamResult<Void>> noSourceFactory(DataFlowRequest request) {
        return completedFuture(StreamResult.error("Unknown data source type: " + request.getSourceDataAddress().getType()));
    }

    @NotNull
    private CompletableFuture<StreamResult<Void>> noSinkFactory(DataFlowRequest request) {
        return completedFuture(StreamResult.error("Unknown data sink type: " + request.getDestinationDataAddress().getType()));
    }


}
