package org.eclipse.slm.resource_management.model.update;

import java.util.List;

public class UpdateInformationResourceType {

    private List<FirmwareVersionDetails> availableFirmwareVersions;

    public List<FirmwareVersionDetails> getAvailableFirmwareVersions() {
        return availableFirmwareVersions;
    }

    public void setAvailableFirmwareVersions(List<FirmwareVersionDetails> availableFirmwareVersions) {
        this.availableFirmwareVersions = availableFirmwareVersions;
    }
}
