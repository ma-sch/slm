package org.eclipse.slm.resource_management.service.discovery.driver.discovery;

import org.eclipse.slm.resource_management.model.discovery.DriverInfo;
import org.eclipse.slm.resource_management.persistence.api.DiscoveryJobRepository;
import org.eclipse.slm.resource_management.service.discovery.driver.DriverRegistryClient;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DriverNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryDriverClientFactory {


    private final DiscoveryJobRepository discoveryJobRepository;

    public DiscoveryDriverClientFactory(DiscoveryJobRepository discoveryJobRepository) {
        this.discoveryJobRepository = discoveryJobRepository;
    }

    public DiscoveryDriverClient createDriverClient(DriverInfo driverInfo){
        return new DiscoveryDriverClient(driverInfo, this.discoveryJobRepository);
    }


}
