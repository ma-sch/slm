package org.eclipse.slm.resource_management.model.update;

import java.util.EnumSet;

public enum FirmwareUpdateStates {
    CREATED,
    PREPARING,
    PREPARED,
    ACTIVATING,
    ACTIVATED,
    CANCELING,
    CANCELED,
    FAILED;

    public static EnumSet<FirmwareUpdateStates> getEndStates() {
        return EnumSet.of(ACTIVATED, CANCELED, FAILED);
    }
}
