package org.eclipse.slm.resource_management.service.discovery.driver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.eclipse.slm.resource_management.model.discovery.DiscoveryJob;
import org.eclipse.slm.resource_management.model.discovery.DriverInfo;
import org.eclipse.slm.resource_management.persistence.api.DiscoveryJobRepository;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DriverNotReachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import siemens.connectivitysuite.drvinfo.v1.ConnSuiteDrvInfo;
import siemens.connectivitysuite.drvinfo.v1.DriverInfoApiGrpc;

public class DriverClient {

    public final static Logger LOG = LoggerFactory.getLogger(DriverClient.class);

    private final DriverInfo driverInfo;

    private final DiscoveryJobRepository discoveryJobRepository;

    private ManagedChannel channel;

    public DriverClient(DriverInfo driverInfo, DiscoveryJobRepository discoveryJobRepository) {
        this.driverInfo = driverInfo;
        this.discoveryJobRepository = discoveryJobRepository;

        this.channel = this.getChannel();
    }

    public DiscoveryJob discover(DiscoveryJobListener discoveryJobListener) {
        var discoverRequest = siemens.industrialassethub.discover.v1.IahDiscover.DiscoverRequest.newBuilder().build();

        var asyncStub = siemens.industrialassethub.discover.v1.DeviceDiscoverApiGrpc.newStub(this.channel);

        var discoverResponseStreamObserver = new DiscoverResponseStreamObserver(this.driverInfo, this.discoveryJobRepository, this.channel);
        discoverResponseStreamObserver.addListener(discoveryJobListener);

        asyncStub.discoverDevices(discoverRequest, discoverResponseStreamObserver);

        var discoveryJob = discoverResponseStreamObserver.getDiscoveryJob();
        this.discoveryJobRepository.save(discoveryJob);

        return discoveryJob;
    }

    public ConnSuiteDrvInfo.VersionInfo getVersionInfo() throws DriverNotReachableException {
        var stub = DriverInfoApiGrpc.newBlockingStub(this.getChannel());

        try {
            var versionInfoResponse = stub.getVersionInfo(ConnSuiteDrvInfo.GetVersionInfoRequest.newBuilder().build());

            return versionInfoResponse.getVersion();
        } catch (Exception e) {
            LOG.debug("Failed to get version info from driver '{}:{}': {}", this.getDriverAddress(), this.getDriverPort(), e.getMessage());
            this.channel.shutdown();
            throw new DriverNotReachableException(this.getDriverAddress(), this.getDriverPort());
        }
    }

    public String getDriverAddress() {
        if (driverInfo.getIpv4Address().isEmpty()) {
            return driverInfo.getDomainName();
        } else {
            return driverInfo.getIpv4Address();
        }
    }

    public int getDriverPort() {
        return this.driverInfo.getPortNumber();
    }

    private ManagedChannel getChannel() {
       var channel = ManagedChannelBuilder.forAddress(this.getDriverAddress(), this.getDriverPort())
               .usePlaintext()
               .build();

       return channel;
    }
}
