package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.resource_management.model.update.FirmwareUpdateEvents;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateStates;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobsJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptor;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class FirmwareUpdateStateMachineInterceptor implements StateMachineInterceptor<FirmwareUpdateStates, FirmwareUpdateEvents> {

    public final static Logger LOG = LoggerFactory.getLogger(FirmwareUpdateStateMachineInterceptor.class);

    private final FirmwareUpdateJobsJpaRepository firmwareUpdateJobsJpaRepository;

    public FirmwareUpdateStateMachineInterceptor(FirmwareUpdateJobsJpaRepository firmwareUpdateJobsJpaRepository) {
        this.firmwareUpdateJobsJpaRepository = firmwareUpdateJobsJpaRepository;
    }

    @Override
    public Message<FirmwareUpdateEvents> preEvent(
            Message<FirmwareUpdateEvents> message,
            StateMachine<FirmwareUpdateStates,
                    FirmwareUpdateEvents> stateMachine) {
        return message;
    }

    @Override
    public void preStateChange(State<FirmwareUpdateStates, FirmwareUpdateEvents> state,
                               Message<FirmwareUpdateEvents> message,
                               Transition<FirmwareUpdateStates,
                               FirmwareUpdateEvents> transition,
                               StateMachine<FirmwareUpdateStates,
                               FirmwareUpdateEvents> stateMachine,
                               StateMachine<FirmwareUpdateStates,
                               FirmwareUpdateEvents> stateMachine1) {
    }

    @Override
    public void postStateChange(State<FirmwareUpdateStates, FirmwareUpdateEvents> state,
                                Message<FirmwareUpdateEvents> message,
                                Transition<FirmwareUpdateStates, FirmwareUpdateEvents> transition,
                                StateMachine<FirmwareUpdateStates, FirmwareUpdateEvents> stateMachine,
                                StateMachine<FirmwareUpdateStates, FirmwareUpdateEvents> stateMachine1) {


        Optional.ofNullable(message)
                .flatMap(msg -> Optional.ofNullable(msg.getHeaders().get("firmwareUpdateJobId", UUID.class)))
                .ifPresent(firmwareUpdateProcessId -> {
                    LOG.info("Updating firmware update process state to: {}", state.getId());
                    var firmwareUpdateProcess = firmwareUpdateJobsJpaRepository.findById(firmwareUpdateProcessId);
                    firmwareUpdateProcess.ifPresent(persistedFirmwareUpdateProcess -> {
                        try {
                            persistedFirmwareUpdateProcess.setFirmwareUpdateState(state.getId());
                            firmwareUpdateJobsJpaRepository.save(persistedFirmwareUpdateProcess);
                        } catch (Exception e) {
                            LOG.error("Error while updating firmware update process state: {}", e.getMessage(), e);
                        }
                    });
                });
    }

    @Override
    public StateContext<FirmwareUpdateStates, FirmwareUpdateEvents> preTransition(
            StateContext<FirmwareUpdateStates, FirmwareUpdateEvents> stateContext) {
        return stateContext;
    }

    @Override
    public StateContext<FirmwareUpdateStates, FirmwareUpdateEvents> postTransition(
            StateContext<FirmwareUpdateStates, FirmwareUpdateEvents> stateContext) {
        return stateContext;
    }

    @Override
    public Exception stateMachineError(
            StateMachine<FirmwareUpdateStates, FirmwareUpdateEvents> stateMachine,
            Exception e) {
        return e;
    }

}
