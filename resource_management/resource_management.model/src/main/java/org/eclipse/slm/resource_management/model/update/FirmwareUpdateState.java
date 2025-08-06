package org.eclipse.slm.resource_management.model.update;

import java.util.EnumSet;

public enum FirmwareUpdateState {
    CREATED,
    PREPARING,
    PREPARED,
    ACTIVATING,
    ACTIVATED,
    CANCELING,
    CANCELED,
    FAILED;

    public static EnumSet<FirmwareUpdateState> getEndStates() {
        return EnumSet.of(ACTIVATED, CANCELED, FAILED);
    }
}
