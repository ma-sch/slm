package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.common.model.exceptions.EventNotAcceptedException;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateEvents;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateStates;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FirmwareUpdateJobStateMachineFactory {

    public final static Logger LOG = LoggerFactory.getLogger(FirmwareUpdateJobStateMachineFactory.class);

    private final FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository;

    private final FirmwareUpdateStateMachineInterceptor firmwareUpdateStateMachineInterceptor;
    private final StateMachineInterceptor stateMachineInterceptor;

    public FirmwareUpdateJobStateMachineFactory(FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository,
                                                FirmwareUpdateStateMachineInterceptor firmwareUpdateStateMachineInterceptor, StateMachineInterceptor stateMachineInterceptor) {
        this.firmwareUpdateJobJpaRepository = firmwareUpdateJobJpaRepository;
        this.firmwareUpdateStateMachineInterceptor = firmwareUpdateStateMachineInterceptor;
        this.stateMachineInterceptor = stateMachineInterceptor;
    }

    public StateMachine<FirmwareUpdateStates, FirmwareUpdateEvents> create(UUID firmwareUpdateJobId) throws Exception {

        var firmwareUpdateJob = firmwareUpdateJobJpaRepository.findById(firmwareUpdateJobId)
                .orElseThrow(() -> new IllegalArgumentException("Firmware update process not found with ID: " + firmwareUpdateJobId));

        var stateMachineBuilder = new StateMachineBuilder.Builder<FirmwareUpdateStates, FirmwareUpdateEvents>();
        stateMachineBuilder.configureStates().withStates()
                .initial(FirmwareUpdateStates.PREPARING)
                .stateEntry(FirmwareUpdateStates.PREPARING, context -> {
                    LOG.info("State: PREPARING | Firmware update preparation started");
                })
                .state(FirmwareUpdateStates.PREPARED)
                .stateEntry(FirmwareUpdateStates.PREPARED, context -> {
                    LOG.info("State: Prepared | Firmware update preparation completed");
                })
                .state(FirmwareUpdateStates.ACTIVATING)
                .state(FirmwareUpdateStates.ACTIVATED)
                .state(FirmwareUpdateStates.CANCELING)
                .state(FirmwareUpdateStates.CANCELED)
                .state(FirmwareUpdateStates.FAILED);

        stateMachineBuilder.configureTransitions()
                .withExternal()
                .source(FirmwareUpdateStates.PREPARING).target(FirmwareUpdateStates.PREPARED).event(FirmwareUpdateEvents.PREPARATION_COMPLETED)

            .and()
                .withExternal()
                .source(FirmwareUpdateStates.PREPARING).target(FirmwareUpdateStates.FAILED).event(FirmwareUpdateEvents.PREPARATION_FAILED)

            .and()
                .withExternal()
                .source(FirmwareUpdateStates.PREPARED).target(FirmwareUpdateStates.ACTIVATING).event(FirmwareUpdateEvents.ACTIVATION_TRIGGERED)
            .and()
                .withExternal()
                .source(FirmwareUpdateStates.PREPARED).target(FirmwareUpdateStates.CANCELING).event(FirmwareUpdateEvents.CANCEL_TRIGGERED)

            .and()
                .withExternal()
                .source(FirmwareUpdateStates.CANCELING).target(FirmwareUpdateStates.CANCELED).event(FirmwareUpdateEvents.CANCEL_COMPLETED)
            .and()
                .withExternal()
                .source(FirmwareUpdateStates.CANCELING).target(FirmwareUpdateStates.FAILED).event(FirmwareUpdateEvents.CANCEL_FAILED)

            .and()
                .withExternal()
                .source(FirmwareUpdateStates.ACTIVATING).target(FirmwareUpdateStates.ACTIVATED).event(FirmwareUpdateEvents.ACTIVATION_COMPLETED)
            .and()
                .withExternal()
                .source(FirmwareUpdateStates.ACTIVATING).target(FirmwareUpdateStates.FAILED).event(FirmwareUpdateEvents.ACTIVATION_FAILED)
            .and()
                .withExternal()
                .source(FirmwareUpdateStates.ACTIVATING).target(FirmwareUpdateStates.CANCELING).event(FirmwareUpdateEvents.CANCEL_TRIGGERED);

    stateMachineBuilder.configureConfiguration().withConfiguration()
        .autoStartup(false)
        .listener(new StateMachineListenerAdapter<>() {
            @Override
            public void eventNotAccepted(Message<FirmwareUpdateEvents> event) {
                LOG.warn("Event not accepted: " + event);
            }

            @Override
            public void stateMachineError(StateMachine<FirmwareUpdateStates, FirmwareUpdateEvents> stateMachine, Exception exception) {
                LOG.error("Error in firmware update state machine: " + exception.getMessage(), exception);
            }

        });

        var stateMachine = stateMachineBuilder.build();
        stateMachine.getStateMachineAccessor().doWithAllRegions( sma -> {
            sma.addStateMachineInterceptor(this.firmwareUpdateStateMachineInterceptor);
            sma.resetStateMachineReactively(new DefaultStateMachineContext<>(firmwareUpdateJob.getFirmwareUpdateState(), null, null, null)).block();
        });
        stateMachine.startReactively().block();

        return stateMachine;
    }

}
