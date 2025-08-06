package org.eclipse.slm.resource_management.service.discovery.driver.discovery;

import com.google.protobuf.ByteString;
import org.eclipse.slm.resource_management.model.discovery.*;
import org.eclipse.slm.resource_management.persistence.api.DiscoveryJobRepository;
import org.eclipse.slm.resource_management.service.discovery.driver.AbstractDriverClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import siemens.common.filters.v1.CommonFilters;
import siemens.common.types.v1.CommonVariant;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryDriverClient extends AbstractDriverClient {

    public final static Logger LOG = LoggerFactory.getLogger(DiscoveryDriverClient.class);

    private final DiscoveryJobRepository discoveryJobRepository;

    public DiscoveryDriverClient(DriverInfo driverInfo, DiscoveryJobRepository discoveryJobRepository) {
        super(driverInfo);
        this.discoveryJobRepository = discoveryJobRepository;
    }

    public DiscoveryJob discover(DiscoveryJobListener discoveryJobListener, DiscoveryRequest discoveryRequest) {
        var discoverRequestBuilder = siemens.industrialassethub.discover.v1.IahDiscover.DiscoverRequest.newBuilder();

        for (var filterValue : discoveryRequest.getFilterValues().entrySet()) {
            var encodedValue = CommonVariant.Variant.newBuilder().setRawData(ByteString.copyFromUtf8(filterValue.getValue()));
            var filter = CommonFilters.ActiveFilter.newBuilder()
                    .setKey(filterValue.getKey())
                    .setValue(encodedValue)
                    .build();
            discoverRequestBuilder.addFilters(filter);
        }

        for (var filterValue : discoveryRequest.getOptionValues().entrySet()) {
            if (filterValue.getValue() != null) {
                var encodedValue = CommonVariant.Variant.newBuilder().setRawData(ByteString.copyFromUtf8(filterValue.getValue()));
                var option = CommonFilters.ActiveOption.newBuilder()
                        .setKey(filterValue.getKey())
                        .setValue(encodedValue)
                        .build();
                discoverRequestBuilder.addOptions(option);
            }
        }

        var asyncStub = siemens.industrialassethub.discover.v1.DeviceDiscoverApiGrpc.newStub(this.channel);

        var discoverResponseStreamObserver = new DiscoverResponseStreamObserver(this.driverInfo, this.discoveryJobRepository, this.channel);
        discoverResponseStreamObserver.addListener(discoveryJobListener);

        LOG.info("Starting discovery job '{}' for driver '{}'", discoverResponseStreamObserver.getDiscoveryJob().getId(), this.driverInfo);

        asyncStub.discoverDevices(discoverRequestBuilder.build(), discoverResponseStreamObserver);

        var discoveryJob = discoverResponseStreamObserver.getDiscoveryJob();
        this.discoveryJobRepository.save(discoveryJob);

        LOG.info("Completed Discovery job '{}' of driver '{}'", discoveryJob.getId(), this.driverInfo);

        return discoveryJob;
    }

    public List<DiscoveryRequestFilter> getDiscoveryRequestFilters () {
        var stub = siemens.industrialassethub.discover.v1.DeviceDiscoverApiGrpc.newBlockingStub(this.channel);

        var discoveryRequestFilters = new ArrayList<DiscoveryRequestFilter>();

        try {
            var filterTypesRequest = CommonFilters.FilterTypesRequest.newBuilder().build();
            var filterTypesResponse = stub.getFilterTypes(filterTypesRequest);

            for (var filterType : filterTypesResponse.getFilterTypesList()) {
                var discoveryRequestFilter = new DiscoveryRequestFilter();
                discoveryRequestFilter.setKey(filterType.getKey());
                discoveryRequestFilter.setDataType(filterType.getDatatype().name());

                discoveryRequestFilters.add(discoveryRequestFilter);
            }
        } catch (io.grpc.StatusRuntimeException e) {
            if (!e.getMessage().equals("UNKNOWN: no supported filters")) {
                LOG.error(e.getMessage());
            }
        }

        return discoveryRequestFilters;
    }

    public List<DiscoveryRequestOption> getDiscoveryRequestOptions() {
        var stub = siemens.industrialassethub.discover.v1.DeviceDiscoverApiGrpc.newBlockingStub(this.channel);

        var discoveryRequestOptions = new ArrayList<DiscoveryRequestOption>();

        try {
            var filterOptionsRequest = CommonFilters.FilterOptionsRequest.newBuilder().build();
            var filterOptionsResponse = stub.getFilterOptions(filterOptionsRequest);

            for (var filterOption : filterOptionsResponse.getFilterOptionsList()) {
                var discoveryRequestOption = new DiscoveryRequestOption();
                discoveryRequestOption.setKey(filterOption.getKey());
                discoveryRequestOption.setDataType(filterOption.getDatatype().name());

                discoveryRequestOptions.add(discoveryRequestOption);
            }
        } catch (io.grpc.StatusRuntimeException e) {
            if (!e.getMessage().equals("UNKNOWN: no supported filter types")) {
                LOG.error(e.getMessage());
            }
        }

        return discoveryRequestOptions;
    }
}
