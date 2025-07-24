package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobJpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FirmwareUpdateJobFactory {

    private final UpdateManager updateManager;
    private final FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository;
    private final FirmwareUpdateJobStateMachineFactory firmwareUpdateJobStateMachineFactory;

    public FirmwareUpdateJobFactory(UpdateManager updateManager,
                                    FirmwareUpdateJobJpaRepository firmwareUpdateJobJpaRepository,
                                    FirmwareUpdateJobStateMachineFactory firmwareUpdateJobStateMachineFactory) {
        this.updateManager = updateManager;
        this.firmwareUpdateJobJpaRepository = firmwareUpdateJobJpaRepository;
        this.firmwareUpdateJobStateMachineFactory = firmwareUpdateJobStateMachineFactory;
    }

    public FirmwareUpdateJob create(UUID resourceId, String softwareNameplateId) throws Exception {

        var firmwareUpdateProcess = new FirmwareUpdateJob(UUID.randomUUID(), resourceId, softwareNameplateId);

        firmwareUpdateProcess = this.firmwareUpdateJobJpaRepository.save(firmwareUpdateProcess);

        firmwareUpdateJobStateMachineFactory.create(firmwareUpdateProcess.getId());

        return firmwareUpdateProcess;
    }
}
