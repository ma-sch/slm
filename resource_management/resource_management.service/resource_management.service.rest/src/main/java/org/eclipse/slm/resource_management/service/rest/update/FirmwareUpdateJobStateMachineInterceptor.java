package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.notification_service.messaging.NotificationMessage;
import org.eclipse.slm.notification_service.messaging.NotificationMessageSender;
import org.eclipse.slm.notification_service.model.EventType;
import org.eclipse.slm.notification_service.model.NotificationCategory;
import org.eclipse.slm.notification_service.model.NotificationSubCategory;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateEvent;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJobStateTransition;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateState;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobJpaRepository;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobStateTransitionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptor;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class FirmwareUpdateJobStateMachineInterceptor implements StateMachineInterceptor<FirmwareUpdateState, FirmwareUpdateEvent> {

    public final static Logger LOG = LoggerFactory.getLogger(FirmwareUpdateJobStateMachineInterceptor.class);

    private final FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository;

    private final FirmwareUpdateJobStateTransitionJpaRepository firmwareUpdateJobStateTransitionJpaRepository;

    private final NotificationMessageSender notificationMessageSender;

    public FirmwareUpdateJobStateMachineInterceptor(FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository,
                                                    FirmwareUpdateJobStateTransitionJpaRepository firmwareUpdateJobStateTransitionJpaRepository,
                                                    NotificationMessageSender notificationMessageSender) {
        this.firmwareUpdateJobJpaRepository = firmwareUpdateJobJpaRepository;
        this.firmwareUpdateJobStateTransitionJpaRepository = firmwareUpdateJobStateTransitionJpaRepository;
        this.notificationMessageSender = notificationMessageSender;
    }

    @Override
    public Message<FirmwareUpdateEvent> preEvent(
            Message<FirmwareUpdateEvent> message,
            StateMachine<FirmwareUpdateState,
                    FirmwareUpdateEvent> stateMachine) {
        return message;
    }

    @Override
    public void preStateChange(State<FirmwareUpdateState, FirmwareUpdateEvent> state,
                               Message<FirmwareUpdateEvent> message,
                               Transition<FirmwareUpdateState,
                                       FirmwareUpdateEvent> transition,
                               StateMachine<FirmwareUpdateState,
                                       FirmwareUpdateEvent> stateMachine,
                               StateMachine<FirmwareUpdateState,
                                       FirmwareUpdateEvent> stateMachine1) {
    }

    @Override
    public void postStateChange(State<FirmwareUpdateState, FirmwareUpdateEvent> state,
                                Message<FirmwareUpdateEvent> message,
                                Transition<FirmwareUpdateState, FirmwareUpdateEvent> transition,
                                StateMachine<FirmwareUpdateState, FirmwareUpdateEvent> stateMachine,
                                StateMachine<FirmwareUpdateState, FirmwareUpdateEvent> stateMachine1) {

        Optional.ofNullable(message)
                .flatMap(msg -> Optional.ofNullable(msg.getHeaders().get("firmwareUpdateJob", FirmwareUpdateJob.class)))
                .ifPresent(firmwareUpdateJob -> {
                    LOG.debug("Updating firmware update job state to: {}", state.getId());
                    try {
                        firmwareUpdateJob.setFirmwareUpdateState(state.getId());
                        this.firmwareUpdateJobJpaRepository.save(firmwareUpdateJob);

                        this.saveFirmwareUpdateJobStateTransition(
                                transition.getSource().getId(),
                                transition.getTarget().getId(),
                                firmwareUpdateJob);

                        var notificationMessage = new NotificationMessage(
                                firmwareUpdateJob.getUserId(),
                                NotificationCategory.RESOURCES, NotificationSubCategory.FIRMWARE_UPDATE, EventType.MODIFIED,
                                firmwareUpdateJob
                        );
                        notificationMessageSender.sendMessage(notificationMessage);
                    } catch (Exception e) {
                        LOG.error("Error while updating firmware update process state: {}", e.getMessage(), e);
                    }
                });
    }

    @Override
    public StateContext<FirmwareUpdateState, FirmwareUpdateEvent> preTransition(
            StateContext<FirmwareUpdateState, FirmwareUpdateEvent> stateContext) {
        return stateContext;
    }

    @Override
    public StateContext<FirmwareUpdateState, FirmwareUpdateEvent> postTransition(
            StateContext<FirmwareUpdateState, FirmwareUpdateEvent> stateContext) {
        return stateContext;
    }

    @Override
    public Exception stateMachineError(
            StateMachine<FirmwareUpdateState, FirmwareUpdateEvent> stateMachine,
            Exception e) {
        return e;
    }

    private void saveFirmwareUpdateJobStateTransition(FirmwareUpdateState sourceState,
                                                      FirmwareUpdateState targetState,
                                                      FirmwareUpdateJob firmwareUpdateJob) {
        try {
            var jobStateTransition = new FirmwareUpdateJobStateTransition(sourceState, targetState, new Date(), firmwareUpdateJob);
            this.firmwareUpdateJobStateTransitionJpaRepository.save(jobStateTransition);
        } catch (Exception e) {
            LOG.error("Error while saving firmware update job state transition: {}", e.getMessage(), e);
        }
    }

}
