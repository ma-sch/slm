package org.eclipse.slm.resource_management.service.rest.update;

import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateState;

public interface FirmwareUpdateJobStateMachineListener {

    void onStateEntry(FirmwareUpdateJob firmwareUpdateJob, FirmwareUpdateState enteredState);

}
