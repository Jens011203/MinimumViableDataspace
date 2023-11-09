/*

Copyright (c) Jens Feser

 */



package org.eclipse.edc;

import org.eclipse.edc.connector.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.connector.core.entity.AbstractStateEntityManager;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.util.sink.OutputStreamDataSinkFactory;
import org.eclipse.edc.manager.DataPlaneManagerWaitImpl;
import org.eclipse.edc.pipeline.PipelineServiceImpl;
import org.eclipse.edc.pipeline.PipelineServiceTransferServiceImpl;
import org.eclipse.edc.registry.TransferServiceRegistryImpl;
import org.eclipse.edc.registry.TransferServiceSelectionStrategy;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.concurrent.Executors;

/**
 * Provides core services for the Data Plane Framework.
 */
@Provides({ DataPlaneManager.class, PipelineService.class, DataTransferExecutorServiceContainer.class, TransferServiceRegistry.class })
@Extension(value = DataPlaneFrameworkExtension.NAME)
public class DataPlaneFrameworkExtension implements ServiceExtension {
    public static final String NAME = "Data Plane Framework";

    @Setting(value = "the iteration wait time in milliseconds in the data plane state machine. Default value " + AbstractStateEntityManager.DEFAULT_ITERATION_WAIT, type = "long")
    private static final String DATAPLANE_MACHINE_ITERATION_WAIT_MILLIS = "edc.dataplane.state-machine.iteration-wait-millis";

    @Setting(value = "the batch size in the data plane state machine. Default value " + AbstractStateEntityManager.DEFAULT_BATCH_SIZE, type = "int")
    private static final String DATAPLANE_MACHINE_BATCH_SIZE = "edc.dataplane.state-machine.batch-size";

    @Setting(value = "how many times a specific operation must be tried before terminating the dataplane with error", type = "int", defaultValue = AbstractStateEntityManager.DEFAULT_SEND_RETRY_LIMIT + "")
    private static final String DATAPLANE_SEND_RETRY_LIMIT = "edc.dataplane.send.retry.limit";

    @Setting(value = "The base delay for the dataplane retry mechanism in millisecond", type = "long", defaultValue = AbstractStateEntityManager.DEFAULT_SEND_RETRY_BASE_DELAY + "")
    private static final String DATAPLANE_SEND_RETRY_BASE_DELAY_MS = "edc.dataplane.send.retry.base-delay.ms";

    @Setting
    private static final String TRANSFER_THREADS = "edc.dataplane.transfer.threads";
    private static final int DEFAULT_TRANSFER_THREADS = 10;
    private DataPlaneManagerWaitImpl dataPlaneManager;

    @Inject
    private TransferServiceSelectionStrategy transferServiceSelectionStrategy;

    @Inject
    private DataPlaneStore store;

    @Inject
    private TransferProcessApiClient transferProcessApiClient;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Inject
    private Telemetry telemetry;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var pipelineService = new PipelineServiceImpl(monitor);
        pipelineService.registerFactory(new OutputStreamDataSinkFactory()); // Added by default to support synchronous data transfer, i.e. pull data
        context.registerService(PipelineService.class, pipelineService);
        var transferService = new PipelineServiceTransferServiceImpl(pipelineService);

        var transferServiceRegistry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);
        transferServiceRegistry.registerTransferService(transferService);
        context.registerService(TransferServiceRegistry.class, transferServiceRegistry);

        var numThreads = context.getSetting(TRANSFER_THREADS, DEFAULT_TRANSFER_THREADS);
        var executorService = Executors.newFixedThreadPool(numThreads);
        var executorContainer = new DataTransferExecutorServiceContainer(
                executorInstrumentation.instrument(executorService, "Data plane transfers"));
        context.registerService(DataTransferExecutorServiceContainer.class, executorContainer);

        var iterationWaitMillis = context.getSetting(DATAPLANE_MACHINE_ITERATION_WAIT_MILLIS, AbstractStateEntityManager.DEFAULT_ITERATION_WAIT);
        var waitStrategy = new ExponentialWaitStrategy(iterationWaitMillis);

        dataPlaneManager = DataPlaneManagerWaitImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .batchSize(context.getSetting(DATAPLANE_MACHINE_BATCH_SIZE, AbstractStateEntityManager.DEFAULT_BATCH_SIZE))
                .clock(clock)
                .entityRetryProcessConfiguration(getEntityRetryProcessConfiguration(context))
                .executorInstrumentation(executorInstrumentation)
                .pipelineService(pipelineService)
                .transferServiceRegistry(transferServiceRegistry)
                .store(store)
                .transferProcessClient(transferProcessApiClient)
                .monitor(monitor)
                .telemetry(telemetry)
                .build();

        context.registerService(DataPlaneManager.class, dataPlaneManager);
    }

    @Override
    public void start() {
        dataPlaneManager.start();
    }

    @Override
    public void shutdown() {
        if (dataPlaneManager != null) {
            dataPlaneManager.stop();
        }
    }

    @NotNull
    private EntityRetryProcessConfiguration getEntityRetryProcessConfiguration(ServiceExtensionContext context) {
        var retryLimit = context.getSetting(DATAPLANE_SEND_RETRY_LIMIT, AbstractStateEntityManager.DEFAULT_SEND_RETRY_LIMIT);
        var retryBaseDelay = context.getSetting(DATAPLANE_SEND_RETRY_BASE_DELAY_MS, AbstractStateEntityManager.DEFAULT_SEND_RETRY_BASE_DELAY);
        return new EntityRetryProcessConfiguration(retryLimit, () -> new ExponentialWaitStrategy(retryBaseDelay));
    }
}