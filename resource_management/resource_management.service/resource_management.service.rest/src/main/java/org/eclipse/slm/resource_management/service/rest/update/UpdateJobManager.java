package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.common.model.exceptions.EventNotAcceptedException;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateEvents;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
public class UpdateJobManager {

    private final static Logger LOG = LoggerFactory.getLogger(UpdateJobManager.class);

    private final FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository;

    private final FirmwareUpdateJobFactory firmwareUpdateJobFactory;

    private final FirmwareUpdateJobStateMachineFactory firmwareUpdateJobStateMachineFactory;

    public UpdateJobManager(FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository, FirmwareUpdateJobFactory firmwareUpdateJobFactory, FirmwareUpdateJobStateMachineFactory firmwareUpdateJobStateMachineFactory) {
        this.firmwareUpdateJobJpaRepository = firmwareUpdateJobJpaRepository;
        this.firmwareUpdateJobFactory = firmwareUpdateJobFactory;
        this.firmwareUpdateJobStateMachineFactory = firmwareUpdateJobStateMachineFactory;
    }

    public List<FirmwareUpdateJob> getFirmwareUpdateJobsOfResource(UUID resourceId) {
        var firmwareUpdateJobs = this.firmwareUpdateJobJpaRepository.findByResourceIdOrderByCreatedAtDesc(resourceId);

        return firmwareUpdateJobs;
    }

    public void startFirmwareUpdateOnResource(UUID resourceId, String softwareNameplateId, String userId) throws Exception {
        this.firmwareUpdateJobFactory.create(resourceId, softwareNameplateId, userId);
    }

    public void changeStateOfFirmwareUpdateOnResource(UUID resourceId, UUID firmwareUpdateJobId, FirmwareUpdateEvents event) throws Exception {
        var firmwareUpdateJobStateMachine = firmwareUpdateJobStateMachineFactory.create(firmwareUpdateJobId);

        Message<FirmwareUpdateEvents> message = MessageBuilder.withPayload(event)
                .setHeader("resourceId", resourceId)
                .setHeader("firmwareUpdateJobId", firmwareUpdateJobId)
                .build();

        var result = firmwareUpdateJobStateMachine.sendEvent(Mono.just(message)).blockFirst();

        if (result.getResultType().equals(StateMachineEventResult.ResultType.DENIED)) {
            var currentState = result.getRegion().getState().getId();
            throw new EventNotAcceptedException(currentState.toString(), event.toString());
        }
    }

}
