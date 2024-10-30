package org.eclipse.slm.resource_management.service.discovery.driver;

import io.grpc.ManagedChannelBuilder;
import org.eclipse.slm.resource_management.model.discovery.DriverInfo;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DriverNotFoundException;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DriverNotReachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import siemens.connectivitysuite.registry.v1.ConnSuiteRegistry;
import siemens.connectivitysuite.registry.v1.RegistryApiGrpc;

import java.util.ArrayList;
import java.util.List;

@Component
public class DriverRegistryClient {

    public final static Logger LOG = LoggerFactory.getLogger(DriverRegistryClient.class);

    private final String driverRegistryAddress;

    private final int driverRegistryPort;

    private final DriverClientFactory driverClientFactory;

    public DriverRegistryClient(@Value("${discovery.driver-registry.address}") String driverRegistryAddress,
                                @Value("${discovery.driver-registry.port}") int driverRegistryPort,
                                DriverClientFactory driverClientFactory) {
        this.driverRegistryAddress = driverRegistryAddress;
        this.driverRegistryPort = driverRegistryPort;
        this.driverClientFactory = driverClientFactory;
    }

    public List<DriverInfo> getRegisteredDrivers() {
        var channel = ManagedChannelBuilder.forAddress(this.driverRegistryAddress, this.driverRegistryPort)
                .usePlaintext()
                .build();

        var registryQueryRequest = ConnSuiteRegistry.QueryRegisteredServicesRequest.newBuilder().build();

        var stub = RegistryApiGrpc.newBlockingStub(channel);
        var response = stub.queryRegisteredServices(registryQueryRequest);

        var driverInfos = new ArrayList<DriverInfo>();
        for (var serviceInfo : response.getInfosList()) {
            var driverInfo = new DriverInfo();
            driverInfo.setInstanceId(serviceInfo.getAppInstanceId());
            driverInfo.setIpv4Address(serviceInfo.getIpv4Address());
            driverInfo.setDomainName(serviceInfo.getDnsDomainname());
            driverInfo.setPortNumber(serviceInfo.getGrpcIpPortNumber());

            try {
                var driverClient = this.driverClientFactory.createDriverClient(driverInfo);
                var versionInfo = driverClient.getVersionInfo();

                driverInfo.setName(versionInfo.getProductName());
                driverInfo.setVendorName(versionInfo.getVendorName());
                var majorVersion = versionInfo.getMajor();
                var minorVersion = versionInfo.getMinor();
                var patchVersion = versionInfo.getPatch();
                driverInfo.setVersion(majorVersion + "." + minorVersion + "." + patchVersion);

                driverInfos.add(driverInfo);
            } catch (DriverNotReachableException e) {
                LOG.warn("Failed to get version info for driver: {}", driverInfo.getInstanceId());
            }
        }

        return driverInfos;
    }

    public DriverInfo getRegisteredDriver(String driverId) throws DriverNotFoundException {
        var optionalDriverInfo = this.getRegisteredDrivers().stream()
                .filter(driverInfo -> driverInfo.getInstanceId().equals(driverId))
                .findAny();

        if (optionalDriverInfo.isPresent()) {
            return optionalDriverInfo.get();
        }
        else {
            throw new DriverNotFoundException(driverId);
        }
    }

}
