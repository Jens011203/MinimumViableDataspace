/*

Copyright (c) Jens Feser

 */


package org.eclipse.edc.manager;

import org.eclipse.edc.connector.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.connector.core.entity.AbstractStateEntityManager;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;


public class DataPlaneManagerWaitImpl extends AbstractStateEntityManager<DataFlow, DataPlaneStore> implements DataPlaneManager {

    private PipelineService pipelineService;
    private TransferServiceRegistry transferServiceRegistry;
    private TransferProcessApiClient transferProcessClient;

    private final ConcurrentHashMap<String, Boolean> runningProcesses = new ConcurrentHashMap<>();

    private DataPlaneManagerWaitImpl() {

    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processDataFlowInState(DataFlowStates.RECEIVED, this::processReceived))
                .processor(processDataFlowInState(DataFlowStates.COMPLETED, this::processCompleted))
                .processor(processDataFlowInState(DataFlowStates.FAILED, this::processFailed));
    }

    @Override
    public Result<Boolean> validate(DataFlowRequest dataRequest) {
        var transferService = transferServiceRegistry.resolveTransferService(dataRequest);
        return transferService != null ?
                transferService.validate(dataRequest) :
                Result.failure(String.format("Cannot find a transfer Service that can handle %s source and %s destination",
                        dataRequest.getSourceDataAddress().getType(), dataRequest.getDestinationDataAddress().getType()));
    }

    @Override
    public void initiate(DataFlowRequest dataRequest) {
        var dataFlow = DataFlow.Builder.newInstance()
                .id(dataRequest.getProcessId())
                .source(dataRequest.getSourceDataAddress())
                .destination(dataRequest.getDestinationDataAddress())
                .callbackAddress(dataRequest.getCallbackAddress())
                .traceContext(telemetry.getCurrentTraceContext())
                .trackable(dataRequest.isTrackable())
                .properties(dataRequest.getProperties())
                .state(DataFlowStates.RECEIVED.code())
                .build();

        update(dataFlow);
    }

    @Override
    public CompletableFuture<StreamResult<Void>> transfer(DataSink sink, DataFlowRequest request) {
        return pipelineService.transfer(sink, request);
    }

    @Override
    public DataFlowStates transferState(String processId) {
        return Optional.ofNullable(store.findById(processId)).map(StatefulEntity::getState)
                .map(DataFlowStates::from).orElse(null);
    }

    private boolean processReceived(DataFlow dataFlow) {
        var request = dataFlow.toRequest();
        var transferService = transferServiceRegistry.resolveTransferService(request);

        if (transferService == null) {
            dataFlow.transitToFailed("No transferService available for DataFlow " + dataFlow.getId());
            update(dataFlow);
            return true;
        }

        if (runningProcesses.containsKey(dataFlow.getId())) {
            System.out.println("Process " + dataFlow.getId() + " is already currently running.");
            return true;
        }

        runningProcesses.put(dataFlow.getId(), true);

        return entityRetryProcessFactory.doAsyncProcess(dataFlow, () -> transferService.transfer(request))

            .entityRetrieve(id -> store.findById(id))
            .onSuccess((f, r) -> {
                if (r.succeeded()) {
                    f.transitToCompleted();
                } else {
                    f.transitToFailed(r.getFailureDetail());
                }
                update(f);
                runningProcesses.remove(f.getId());
            })
            .onFailure((f, t) -> {
                f.transitToReceived();
                update(f);
                runningProcesses.remove(f.getId());

            })
            .onRetryExhausted((f, t) -> {
                f.transitToFailed(t.getMessage());
                update(f);
                runningProcesses.remove(f.getId());
            })
            .execute("start data flow");
    }

    private boolean processCompleted(DataFlow dataFlow) {
        var response = transferProcessClient.completed(dataFlow.toRequest());
        if (response.succeeded()) {
            dataFlow.transitToNotified();
            update(dataFlow);
        } else {
            dataFlow.transitToCompleted();
            update(dataFlow);
        }
        return true;
    }

    private boolean processFailed(DataFlow dataFlow) {
        var response = transferProcessClient.failed(dataFlow.toRequest(), dataFlow.getErrorDetail());
        if (response.succeeded()) {
            dataFlow.transitToNotified();
            update(dataFlow);
        } else {
            dataFlow.transitToFailed(dataFlow.getErrorDetail());
            update(dataFlow);
        }
        return true;
    }

    private Processor processDataFlowInState(DataFlowStates state, Function<DataFlow, Boolean> function) {
        var filter = new Criterion[]{ StateEntityStore.hasState(state.code()) };
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                .onNotProcessed(this::breakLease)
                .build();
    }

    public static class Builder extends AbstractStateEntityManager.Builder<DataFlow, DataPlaneStore, org.eclipse.edc.manager.DataPlaneManagerWaitImpl, org.eclipse.edc.manager.DataPlaneManagerWaitImpl.Builder> {

        public static org.eclipse.edc.manager.DataPlaneManagerWaitImpl.Builder newInstance() {
            return new org.eclipse.edc.manager.DataPlaneManagerWaitImpl.Builder();
        }

        private Builder() {
            super(new org.eclipse.edc.manager.DataPlaneManagerWaitImpl());
        }

        @Override
        public org.eclipse.edc.manager.DataPlaneManagerWaitImpl.Builder self() {
            return this;
        }

        public org.eclipse.edc.manager.DataPlaneManagerWaitImpl.Builder pipelineService(PipelineService pipelineService) {
            manager.pipelineService = pipelineService;
            return this;
        }

        public org.eclipse.edc.manager.DataPlaneManagerWaitImpl.Builder transferServiceRegistry(TransferServiceRegistry transferServiceRegistry) {
            manager.transferServiceRegistry = transferServiceRegistry;
            return this;
        }

        public org.eclipse.edc.manager.DataPlaneManagerWaitImpl.Builder transferProcessClient(TransferProcessApiClient transferProcessClient) {
            manager.transferProcessClient = transferProcessClient;
            return this;
        }

        public org.eclipse.edc.manager.DataPlaneManagerWaitImpl build() {
            Objects.requireNonNull(manager.transferProcessClient);
            return manager;
        }
    }

}
