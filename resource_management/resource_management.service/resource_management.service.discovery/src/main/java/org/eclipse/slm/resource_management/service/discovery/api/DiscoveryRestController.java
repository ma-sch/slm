package org.eclipse.slm.resource_management.service.discovery.api;

import org.eclipse.slm.resource_management.model.discovery.DiscoveredResource;
import org.eclipse.slm.resource_management.model.discovery.DiscoveryJob;
import org.eclipse.slm.resource_management.model.discovery.DriverInfo;
import org.eclipse.slm.resource_management.persistence.api.DiscoveryJobRepository;
import org.eclipse.slm.resource_management.service.discovery.DiscoveryHandler;
import org.eclipse.slm.resource_management.service.discovery.driver.DriverClientFactory;
import org.eclipse.slm.resource_management.service.discovery.driver.DriverRegistryClient;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DiscoveryJobNotFoundException;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DriverNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/resources/discovery")
public class DiscoveryRestController {

    private final DriverRegistryClient driverRegistryClient;

    private final DiscoveryJobRepository discoveryJobRepository;

    private final DriverClientFactory driverClientFactory;

    private final DiscoveryHandler discoveryHandler;

    public DiscoveryRestController(DriverRegistryClient driverRegistryClient,
                                   DiscoveryJobRepository discoveryJobRepository,
                                   DriverClientFactory driverClientFactory, DiscoveryHandler discoveryHandler) {
        this.driverRegistryClient = driverRegistryClient;
        this.discoveryJobRepository = discoveryJobRepository;
        this.driverClientFactory = driverClientFactory;
        this.discoveryHandler = discoveryHandler;
    }

    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public ResponseEntity<Collection<DiscoveryJob>> getDiscoveryJobs() {
        var discoveryJobs = this.discoveryJobRepository.findAll();

        return ResponseEntity.ok(discoveryJobs);
    }

    @RequestMapping(value = "/jobs/{discoveryJobId}", method = RequestMethod.GET)
    public ResponseEntity<DiscoveryJob> getDiscoveryJob(@PathVariable(name = "discoveryJobId") UUID discoveryJobId)
            throws DiscoveryJobNotFoundException {
        var discoveryJobOptional = this.discoveryJobRepository.findById(discoveryJobId);

        if (discoveryJobOptional.isPresent()) {
            return ResponseEntity.ok(discoveryJobOptional.get());
        }
        else {
            throw new DiscoveryJobNotFoundException(discoveryJobId);
        }

    }

    @RequestMapping(value = "/drivers", method = RequestMethod.GET)
    public ResponseEntity<List<DriverInfo>> getRegisteredDrivers() {
        var driverInfos = this.driverRegistryClient.getRegisteredDrivers();

        return ResponseEntity.ok(driverInfos);
    }

    @RequestMapping(value = "/drivers/{driverId}/discover", method = RequestMethod.GET)
    public ResponseEntity<DiscoveryJobStartedResponse> discover(@PathVariable(name = "driverId") String driverId)
            throws DriverNotFoundException {
        var jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        var discoveryJobId = this.discoveryHandler.discover(jwtAuthenticationToken, driverId);

        var response = new DiscoveryJobStartedResponse(discoveryJobId);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/inbox", method = RequestMethod.GET)
    public ResponseEntity<Collection<DiscoveredResource>> getDiscoveredResources(
            @RequestParam(name = "removeDuplicate", required = false, defaultValue = "false") boolean removeDuplicate,
            @RequestParam(name = "onlyLatestJobs", required = false, defaultValue = "false") boolean onlyLatestJobs,
            @RequestParam(name = "includeIgnored", required = false, defaultValue = "false") boolean includeIgnored
    ) throws InterruptedException {
        var discoveredResources = this.discoveryHandler.getDiscoveredResources(removeDuplicate, onlyLatestJobs, includeIgnored);

        Thread.sleep(1000);

        return ResponseEntity.ok(discoveredResources);
    }
}
