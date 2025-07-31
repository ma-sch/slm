package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateStates;
import org.eclipse.slm.resource_management.model.update.exceptions.FirmwareUpdateAlreadyInProgressException;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobJpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FirmwareUpdateJobFactory {

    private final FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository;
    private final FirmwareUpdateJobStateMachineFactory firmwareUpdateJobStateMachineFactory;

    public FirmwareUpdateJobFactory(FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository,
                                    FirmwareUpdateJobStateMachineFactory firmwareUpdateJobStateMachineFactory) {
        this.firmwareUpdateJobJpaRepository = firmwareUpdateJobJpaRepository;
        this.firmwareUpdateJobStateMachineFactory = firmwareUpdateJobStateMachineFactory;
    }

    public FirmwareUpdateJob create(UUID resourceId, String softwareNameplateId, String userId) throws Exception {
        // Check if firmware update is already in progress for the resource (by checking the state of the latest job)
        var firmwareUpdateJobsOfResource = firmwareUpdateJobJpaRepository.findByResourceIdOrderByCreatedAtDesc(resourceId);
        if (firmwareUpdateJobsOfResource.isEmpty()
            || FirmwareUpdateStates.getEndStates().contains(firmwareUpdateJobsOfResource.get(0).getFirmwareUpdateState())) {
            var firmwareUpdateProcess = new FirmwareUpdateJob(UUID.randomUUID(), resourceId, softwareNameplateId, userId);

            firmwareUpdateProcess = this.firmwareUpdateJobJpaRepository.save(firmwareUpdateProcess);

            firmwareUpdateJobStateMachineFactory.create(firmwareUpdateProcess.getId());

            return firmwareUpdateProcess;
        }
        else {
            throw new FirmwareUpdateAlreadyInProgressException(resourceId);
        }

    }
}
