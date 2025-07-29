package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.resource_management.model.update.FirmwareUpdateEvents;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateStates;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobJpaRepository;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class FirmwareUpdateJobStateMachineFactory {

    public final static Logger LOG = LoggerFactory.getLogger(FirmwareUpdateJobStateMachineFactory.class);

    private final FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository;

    private final FirmwareUpdateStateMachineInterceptor firmwareUpdateStateMachineInterceptor;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final List<FirmwareUpdateStates> endStates = List.of(FirmwareUpdateStates.CANCELED, FirmwareUpdateStates.ACTIVATED, FirmwareUpdateStates.FAILED);

    public FirmwareUpdateJobStateMachineFactory(FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository,
                                                FirmwareUpdateStateMachineInterceptor firmwareUpdateStateMachineInterceptor) {
        this.firmwareUpdateJobJpaRepository = firmwareUpdateJobJpaRepository;
        this.firmwareUpdateStateMachineInterceptor = firmwareUpdateStateMachineInterceptor;
    }

    public StateMachine<FirmwareUpdateStates, FirmwareUpdateEvents> create(UUID firmwareUpdateJobId) throws Exception {

        var firmwareUpdateJob = firmwareUpdateJobJpaRepository.findById(firmwareUpdateJobId)
                .orElseThrow(() -> new IllegalArgumentException("Firmware update process not found with ID: " + firmwareUpdateJobId));

        var stateMachineBuilder = new StateMachineBuilder.Builder<FirmwareUpdateStates, FirmwareUpdateEvents>();
        stateMachineBuilder.configureStates().withStates()
                .initial(FirmwareUpdateStates.CREATED, context -> {
                    LOG.info("State Machine for Firmware Update Job {} created with initial state: CREATED", firmwareUpdateJobId);
                })
                .state(FirmwareUpdateStates.PREPARING)
                .stateEntry(FirmwareUpdateStates.PREPARING, context -> {
                    LOG.info("State: PREPARING | Firmware update preparation started");
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(5000);
                            transitionStateMachineToState(context.getStateMachine(), firmwareUpdateJobId, FirmwareUpdateEvents.PREPARATION_COMPLETED);
                        } catch (InterruptedException e) {
                            transitionStateMachineToState(context.getStateMachine(), firmwareUpdateJobId, FirmwareUpdateEvents.PREPARATION_FAILED);
                        }
                    }, executor);
                })
                .state(FirmwareUpdateStates.PREPARED)
                .stateEntry(FirmwareUpdateStates.PREPARED, context -> {
                    LOG.info("State: Prepared | Firmware update preparation completed");
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(5000);
                            transitionStateMachineToState(context.getStateMachine(), firmwareUpdateJobId, FirmwareUpdateEvents.ACTIVATION_TRIGGERED);
                        } catch (InterruptedException e) {
                            transitionStateMachineToState(context.getStateMachine(), firmwareUpdateJobId, FirmwareUpdateEvents.CANCEL_TRIGGERED);
                        }
                    }, executor);
                })
                .state(FirmwareUpdateStates.ACTIVATING)
                .stateEntry(FirmwareUpdateStates.ACTIVATING, context -> {
                    LOG.info("State: ACTIVATING | Firmware update activation started");
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(5000);
                            transitionStateMachineToState(context.getStateMachine(), firmwareUpdateJobId, FirmwareUpdateEvents.ACTIVATION_COMPLETED);
                        } catch (InterruptedException e) {
                            transitionStateMachineToState(context.getStateMachine(), firmwareUpdateJobId, FirmwareUpdateEvents.ACTIVATION_FAILED);
                        }
                    }, executor);
                })
                .state(FirmwareUpdateStates.ACTIVATED)
                .stateEntry(FirmwareUpdateStates.ACTIVATED, context -> {
                    LOG.info("State: ACTIVATED | Firmware update activation completed");
                })

                .state(FirmwareUpdateStates.CANCELING)
                .stateEntry(FirmwareUpdateStates.CANCELING, context -> {
                    LOG.info("State: CANCELING | Firmware update cancellation started");
                })
                .state(FirmwareUpdateStates.CANCELED)
                .stateEntry(FirmwareUpdateStates.CANCELED, context -> {
                    LOG.info("State: CANCELED | Firmware update canceled");
                })

                .state(FirmwareUpdateStates.FAILED)
                .stateEntry(FirmwareUpdateStates.FAILED, context -> {
                    LOG.error("State: FAILED | Firmware update process failed");
                });

                for (var endState : endStates) {
                    stateMachineBuilder.configureStates().withStates()
                        .end(endState)
                        .stateEntry(endState, context -> {
                            LOG.info("State Machine for Firmware Update Job {} reached end state: {}", firmwareUpdateJobId, endState);
                        });
                }

        stateMachineBuilder.configureTransitions()
                .withExternal()
                .source(FirmwareUpdateStates.CREATED).target(FirmwareUpdateStates.PREPARING).event(FirmwareUpdateEvents.PREPARATION_TRIGGERED)

            .and()
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

        // Trigger transition to PREPARING state if the state machine is in the initial state because stateEntry
        // actions are not executed when the state machine is started from the initial state.
        if (stateMachine.getState().getId().equals(FirmwareUpdateStates.CREATED)) {
            this.transitionStateMachineToState(stateMachine, firmwareUpdateJobId, FirmwareUpdateEvents.PREPARATION_TRIGGERED);
        }

        return stateMachine;
    }

    private void transitionStateMachineToState(StateMachine<FirmwareUpdateStates, FirmwareUpdateEvents> stateMachine,
                             UUID firmwareUpdateJobId, FirmwareUpdateEvents targetState) {
        Message<FirmwareUpdateEvents> message = MessageBuilder.withPayload(targetState)
                .setHeader("firmwareUpdateJobId", firmwareUpdateJobId)
                .build();

        var result = stateMachine.sendEvent(Mono.just(message)).blockFirst();
    }

}
