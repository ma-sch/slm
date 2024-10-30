package org.eclipse.slm.resource_management.service.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.eclipse.slm.resource_management.service.discovery.utils.DataFormatUtil;
import org.eclipse.slm.resource_management.service.discovery.utils.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class GrpcTest {

    @Test
    public void testParsing() throws JsonProcessingException {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("10.17.167.87", 8080)
                .usePlaintext()
                .build();

        var stub = siemens.industrialassethub.discover.v1.DeviceDiscoverApiGrpc.newBlockingStub(channel);

        var discoverResponseIterator = stub.discoverDevices(siemens.industrialassethub.discover.v1.IahDiscover.DiscoverRequest.newBuilder().build());

        var discoverResponse = discoverResponseIterator.next();

        var discoveredDevice1 = discoverResponse.getDevices(0);


        var objectMapper = new ObjectMapper();
//        var json = this.converToJson(discoveredDevice1.getIdentifiers(9));
//        var jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
//        System.out.println(jsonString);

        var jsonIdentifiers = new ArrayList<JsonNode>();
        for (var deviceIdentifier: discoveredDevice1.getIdentifiersList()) {
            var json = DataFormatUtil.convertToJson(deviceIdentifier);

            var identifierName = deviceIdentifier.getClassifiers(0).getValue().replace("https://schema.industrial-assets.io/base/v0.8.3/Asset#", "");
            if (identifierName.contains("/")) {
                jsonIdentifiers.add(json);
            }
            else {
                var identifierObjectNode = objectMapper.createObjectNode();
                identifierObjectNode.put(identifierName, json);
                jsonIdentifiers.add(identifierObjectNode);
            }
        }

        var deviceJson = objectMapper.createObjectNode();
        for (var jsonIdentifier: jsonIdentifiers) {
            deviceJson = (ObjectNode) JsonUtil.merge(deviceJson, jsonIdentifier);
            System.out.println(jsonIdentifier);
        }

        var jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deviceJson);
        System.out.println(jsonString);

        channel.shutdown();
    }

    @Test
    public void testSegmentResolve() {
        var jsonNode = DataFormatUtil.resolvePropertyNameSegments("product_instance_identifier/manufacturer_product/manufacturer/name", "testName");

        System.out.println(jsonNode);
    }



}
