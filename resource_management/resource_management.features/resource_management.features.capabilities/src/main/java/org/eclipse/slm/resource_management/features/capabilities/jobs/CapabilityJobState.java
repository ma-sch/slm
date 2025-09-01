package org.eclipse.slm.resource_management.features.capabilities.jobs;

import java.util.EnumSet;

public enum CapabilityJobState {
    CREATED,
    INSTALLING,
    INSTALLED,
    UNINSTALLING,
    UNINSTALLED,
    FAILED;

    public static EnumSet<CapabilityJobState> getEndStates() {
        return EnumSet.of(UNINSTALLED, FAILED);
    }
}
