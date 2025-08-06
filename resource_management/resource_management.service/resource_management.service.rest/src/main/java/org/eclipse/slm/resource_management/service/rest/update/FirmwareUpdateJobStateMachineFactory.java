package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.resource_management.model.update.FirmwareUpdateEvent;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.eclipse.slm.resource_management.model.update.FirmwareUpdateEvent.PREPARATION_TRIGGERED;

@Service
public class FirmwareUpdateJobStateMachineFactory {

    public final static Logger LOG = LoggerFactory.getLogger(FirmwareUpdateJobStateMachineFactory.class);

    private final List<FirmwareUpdateState> endStates = List.of(FirmwareUpdateState.CANCELED, FirmwareUpdateState.ACTIVATED, FirmwareUpdateState.FAILED);

    private final FirmwareUpdateJobStateMachineInterceptor firmwareUpdateJobStateMachineInterceptor;

    public FirmwareUpdateJobStateMachineFactory(FirmwareUpdateJobStateMachineInterceptor firmwareUpdateJobStateMachineInterceptor) {
        this.firmwareUpdateJobStateMachineInterceptor = firmwareUpdateJobStateMachineInterceptor;
    }

    public StateMachine<FirmwareUpdateState, FirmwareUpdateEvent> create(FirmwareUpdateJob firmwareUpdateJob,
                                                                         FirmwareUpdateJobStateMachineListener firmwareUpdateJobStateMachineListener) throws Exception {
        var stateMachineBuilder = new StateMachineBuilder.Builder<FirmwareUpdateState, FirmwareUpdateEvent>();
        stateMachineBuilder.configureStates().withStates()
                .initial(FirmwareUpdateState.CREATED, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, FirmwareUpdateState.CREATED);
                })

                .state(FirmwareUpdateState.PREPARING)
                .stateEntry(FirmwareUpdateState.PREPARING, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, FirmwareUpdateState.PREPARING);
                })
                .state(FirmwareUpdateState.PREPARED)
                .stateEntry(FirmwareUpdateState.PREPARED, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, FirmwareUpdateState.PREPARED);
                })

                .state(FirmwareUpdateState.ACTIVATING)
                .stateEntry(FirmwareUpdateState.ACTIVATING, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, FirmwareUpdateState.ACTIVATING);
                })
                .state(FirmwareUpdateState.ACTIVATED)
                .stateEntry(FirmwareUpdateState.ACTIVATED, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, FirmwareUpdateState.ACTIVATED);
                })

                .state(FirmwareUpdateState.CANCELING)
                .stateEntry(FirmwareUpdateState.CANCELING, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, FirmwareUpdateState.CANCELING);
                })
                .state(FirmwareUpdateState.CANCELED)
                .stateEntry(FirmwareUpdateState.CANCELED, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, FirmwareUpdateState.CANCELED);
                })

                .state(FirmwareUpdateState.FAILED)
                .stateEntry(FirmwareUpdateState.FAILED, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, FirmwareUpdateState.FAILED);
                });

        for (var endState : endStates) {
            stateMachineBuilder.configureStates().withStates()
                .end(endState)
                .stateEntry(endState, context -> {
                    firmwareUpdateJobStateMachineListener.onStateEntry(firmwareUpdateJob, endState);
                });
        }

        stateMachineBuilder.configureTransitions()
                .withExternal()
                .source(FirmwareUpdateState.CREATED).target(FirmwareUpdateState.PREPARING).event(PREPARATION_TRIGGERED)

            .and()
                .withExternal()
                .source(FirmwareUpdateState.PREPARING).target(FirmwareUpdateState.PREPARED).event(FirmwareUpdateEvent.PREPARATION_COMPLETED)
            .and()
                .withExternal()
                .source(FirmwareUpdateState.PREPARING).target(FirmwareUpdateState.FAILED).event(FirmwareUpdateEvent.PREPARATION_FAILED)

            .and()
                .withExternal()
                .source(FirmwareUpdateState.PREPARED).target(FirmwareUpdateState.ACTIVATING).event(FirmwareUpdateEvent.ACTIVATION_TRIGGERED)
            .and()
                .withExternal()
                .source(FirmwareUpdateState.PREPARED).target(FirmwareUpdateState.CANCELING).event(FirmwareUpdateEvent.CANCEL_TRIGGERED)

            .and()
                .withExternal()
                .source(FirmwareUpdateState.CANCELING).target(FirmwareUpdateState.CANCELED).event(FirmwareUpdateEvent.CANCEL_COMPLETED)
            .and()
                .withExternal()
                .source(FirmwareUpdateState.CANCELING).target(FirmwareUpdateState.FAILED).event(FirmwareUpdateEvent.CANCEL_FAILED)

            .and()
                .withExternal()
                .source(FirmwareUpdateState.ACTIVATING).target(FirmwareUpdateState.ACTIVATED).event(FirmwareUpdateEvent.ACTIVATION_COMPLETED)
            .and()
                .withExternal()
                .source(FirmwareUpdateState.ACTIVATING).target(FirmwareUpdateState.FAILED).event(FirmwareUpdateEvent.ACTIVATION_FAILED)
            .and()
                .withExternal()
                .source(FirmwareUpdateState.ACTIVATING).target(FirmwareUpdateState.CANCELING).event(FirmwareUpdateEvent.CANCEL_TRIGGERED);

    stateMachineBuilder.configureConfiguration().withConfiguration()
        .autoStartup(false)
        .listener(new StateMachineListenerAdapter<>() {
            @Override
            public void eventNotAccepted(Message<FirmwareUpdateEvent> event) {
                LOG.warn("Event not accepted: " + event);
            }

            @Override
            public void stateMachineError(StateMachine<FirmwareUpdateState, FirmwareUpdateEvent> stateMachine, Exception exception) {
                LOG.error("Error in firmware update state machine: " + exception.getMessage(), exception);
            }

        });

        var stateMachine = stateMachineBuilder.build();
        stateMachine.getStateMachineAccessor().doWithAllRegions( sma -> {
            sma.addStateMachineInterceptor(this.firmwareUpdateJobStateMachineInterceptor);
            sma.resetStateMachineReactively(new DefaultStateMachineContext<>(firmwareUpdateJob.getFirmwareUpdateState(), null, null, null)).block();
        });
        stateMachine.startReactively().block();

        return stateMachine;
    }

}
