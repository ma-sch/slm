package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;

import java.util.List;
import java.util.UUID;

public interface FirmwareUpdateJobService {

    public List<FirmwareUpdateJob> getFirmwareUpdateJobsOfResource(UUID resourceId);

    void initFirmwareUpdate(UUID resourceId, String softwareNameplateId, String userId) throws Exception;

    void activateFirmwareUpdate(UUID firmwareUpdateJobId) throws Exception;

    void cancelFirmwareUpdate(UUID firmwareUpdateJobId) throws Exception;

}
