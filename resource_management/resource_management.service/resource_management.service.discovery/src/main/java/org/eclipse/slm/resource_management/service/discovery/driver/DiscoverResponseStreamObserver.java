package org.eclipse.slm.resource_management.service.discovery.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.eclipse.slm.resource_management.model.discovery.DiscoveredResource;
import org.eclipse.slm.resource_management.model.discovery.DiscoveryJob;
import org.eclipse.slm.resource_management.model.discovery.DiscoveryJobState;
import org.eclipse.slm.resource_management.model.discovery.DriverInfo;
import org.eclipse.slm.resource_management.persistence.api.DiscoveryJobRepository;
import org.eclipse.slm.resource_management.service.discovery.utils.DataFormatUtil;
import org.eclipse.slm.resource_management.service.discovery.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import siemens.industrialassethub.discover.v1.IahDiscover;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DiscoverResponseStreamObserver implements StreamObserver<IahDiscover.DiscoverResponse> {

    public final static Logger LOG = LoggerFactory.getLogger(DiscoverResponseStreamObserver.class);

    private DiscoveryJob discoveryJob;

    private ObjectMapper objectMapper;

    private List<DiscoveredResource> discoveredResources = new ArrayList<>();

    private final DiscoveryJobRepository discoveryJobRepository;

    private final DriverInfo driverInfo;

    private final ManagedChannel channel;

    private final List<DiscoveryJobListener> listeners = new ArrayList<>();

    public DiscoverResponseStreamObserver(DriverInfo driverInfo,
                                          DiscoveryJobRepository discoveryJobRepository,
                                          ManagedChannel channel) {
        this.driverInfo = driverInfo;
        this.channel = channel;
        this.objectMapper = new ObjectMapper();
        this.discoveryJobRepository = discoveryJobRepository;

        this.discoveryJob = new DiscoveryJob(this.driverInfo.getInstanceId());
        this.discoveryJobRepository.save(this.discoveryJob);
    }

    public DiscoveryJob getDiscoveryJob() {
        return discoveryJob;
    }

    @Override
    public void onNext(IahDiscover.DiscoverResponse discoverResponse) {
        LOG.debug("Received discovery response: {}", discoverResponse);

        for (var discoveredDevice : discoverResponse.getDevicesList()) {
            // Convert identifiers to JSON
            var jsonIdentifiers = new ArrayList<JsonNode>();
            for (var deviceIdentifier : discoveredDevice.getIdentifiersList()) {
                var json = DataFormatUtil.convertToJson(deviceIdentifier);

                var identifierName = deviceIdentifier.getClassifiers(0).getValue().replace(DataFormatUtil.CLASSIFIER_PREFIX, "");
                if (identifierName.contains("/")) {
                    jsonIdentifiers.add(json);
                } else {
                    var identifierObjectNode = objectMapper.createObjectNode();
                    identifierObjectNode.put(identifierName, json);
                    jsonIdentifiers.add(identifierObjectNode);
                }
            }

            // Merge converted JSON identifiers into a single JSON object
            var deviceJson = objectMapper.createObjectNode();
            for (var jsonIdentifier : jsonIdentifiers) {
                if (jsonIdentifier.has("software_components")) {
                    var softwareComponentName = jsonIdentifier.get("software_components").get("artifact").get("software_identifier").get("name").asText();
                    if (jsonIdentifier.get("software_components").get("artifact").get("software_identifier").has("version")) {
                        var softwareComponentVersion = jsonIdentifier.get("software_components").get("artifact").get("software_identifier").get("version").asText();
                        if (deviceJson.has("software_components")) {
                            var softwareComponents = (ObjectNode) deviceJson.get("software_components");
                            softwareComponents.put(softwareComponentName, softwareComponentVersion);
                        } else {

                            var softwareComponents = objectMapper.createObjectNode();
                            softwareComponents.put(softwareComponentName, softwareComponentVersion);
                            deviceJson.put("software_components", softwareComponents);
                        }
                    }
                }
                else {
                    deviceJson = (ObjectNode) JsonUtil.merge(deviceJson, jsonIdentifier);
                }
            }

            var discoveredResource = DataFormatUtil.convertFromJsonToDiscoveredResource(this.driverInfo, deviceJson);
            discoveredResources.add(discoveredResource);
        }

    }

    @Override
    public void onError(Throwable throwable) {
        LOG.error("Error during discovery", throwable);
        this.discoveryJob.setFinishDate(Calendar.getInstance().getTime());
        this.discoveryJob.setState(DiscoveryJobState.FAILED);
        this.discoveryJobRepository.save(this.discoveryJob);

        this.channel.shutdown();
    }

    @Override
    public void onCompleted() {
        LOG.info("Discovery completed");
        this.discoveryJob.setFinishDate(Calendar.getInstance().getTime());
        this.discoveryJob.setState(DiscoveryJobState.COMPLETED);
        this.discoveryJob.setDiscoveryResult(this.discoveredResources);
        this.discoveryJobRepository.save(this.discoveryJob);

        this.channel.shutdown();

        for (var listener : listeners) {
            listener.onDiscoveryCompleted(discoveryJob);
        }
    }

    public void addListener(DiscoveryJobListener discoveryJobListener) {
        this.listeners.add(discoveryJobListener);
    }
}
