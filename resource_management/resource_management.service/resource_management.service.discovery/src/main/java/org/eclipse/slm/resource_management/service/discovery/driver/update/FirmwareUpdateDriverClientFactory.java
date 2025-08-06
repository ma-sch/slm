package org.eclipse.slm.resource_management.service.discovery.driver.update;

import org.eclipse.slm.resource_management.model.discovery.DriverInfo;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DriverNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class FirmwareUpdateDriverClientFactory {

    public FirmwareUpdateDriverClientFactory() {
    }

    public FirmwareUpdateDriverClient createDriverClient(DriverInfo driverInfo) throws DriverNotFoundException {
        return new FirmwareUpdateDriverClient(driverInfo);
    }

}
