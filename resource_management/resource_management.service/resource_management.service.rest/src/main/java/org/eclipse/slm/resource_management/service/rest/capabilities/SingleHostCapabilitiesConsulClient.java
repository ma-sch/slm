package org.eclipse.slm.resource_management.service.rest.capabilities;

import org.eclipse.slm.common.consul.client.ConsulCredential;
import org.eclipse.slm.common.consul.client.apis.*;
import org.eclipse.slm.common.consul.model.catalog.Node;
import org.eclipse.slm.common.consul.model.catalog.NodeService;
import org.eclipse.slm.common.consul.model.exceptions.ConsulLoginFailedException;
import org.eclipse.slm.resource_management.model.capabilities.Capability;
import org.eclipse.slm.resource_management.model.capabilities.CapabilityHealthCheck;
import org.eclipse.slm.resource_management.model.consul.capability.CapabilityService;
import org.eclipse.slm.resource_management.model.consul.capability.CapabilityServiceStatus;
import org.eclipse.slm.resource_management.model.consul.capability.SingleHostCapabilityService;
import org.eclipse.slm.resource_management.model.resource.exceptions.ResourceNotFoundException;
import org.eclipse.slm.resource_management.persistence.api.CapabilityJpaRepository;
import org.eclipse.slm.resource_management.service.rest.resources.ResourcesConsulClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

@Component
public class SingleHostCapabilitiesConsulClient {
    private final static Logger LOG = LoggerFactory.getLogger(SingleHostCapabilitiesConsulClient.class);

    private final ResourcesConsulClient resourcesConsulClient;
    private final ConsulNodesApiClient consulNodesApiClient;
    private final ConsulServicesApiClient consulServicesApiClient;
    private final ConsulAclApiClient consulAclApiClient;
    private final ConsulHealthApiClient consulHealthApiClient;
    private final ConsulKeyValueApiClient consulKeyValueApiClient;
    private final CapabilityUtil capabilityUtil;
    private final CapabilityJpaRepository capabilityJpaRepository;


    public SingleHostCapabilitiesConsulClient(
            ResourcesConsulClient resourcesConsulClient,
            ConsulNodesApiClient consulNodesApiClient,
            ConsulServicesApiClient consulServicesApiClient,
            ConsulAclApiClient consulAclApiClient,
            ConsulHealthApiClient consulHealthApiClient,
            ConsulKeyValueApiClient consulKeyValueApiClient,
            CapabilityUtil capabilityUtil,
            CapabilityJpaRepository capabilityJpaRepository
    ) {
        this.resourcesConsulClient = resourcesConsulClient;
        this.consulNodesApiClient = consulNodesApiClient;
        this.consulServicesApiClient = consulServicesApiClient;
        this.consulAclApiClient = consulAclApiClient;
        this.consulHealthApiClient = consulHealthApiClient;
        this.consulKeyValueApiClient = consulKeyValueApiClient;
        this.capabilityUtil = capabilityUtil;
        this.capabilityJpaRepository = capabilityJpaRepository;
    }

    //region ADD Function
    public void addSingleHostCapabilityWithHealthCheckToAllConsulNodes(
            ConsulCredential consulCredential,
            Capability capability,
            Boolean isManaged,
            Map<String, String> configParameter
    ) throws ConsulLoginFailedException, ResourceNotFoundException, IllegalAccessException {
        if (capability.getHealthCheck() != null) {
            var existingResources = this.resourcesConsulClient.getResources(consulCredential);
            for (var existingResource : existingResources) {
                this.addSingleHostCapabilityToNode(
                        consulCredential,
                        capability,
                        existingResource.getId(),
                        CapabilityServiceStatus.INSTALL,
                        isManaged,
                        configParameter
                );
            }
        }
        else {
            LOG.error("Capability " + capability + "has no health check defined");
        }
    }

    public CapabilityService addSingleHostCapabilityToNode(
            ConsulCredential consulCredential,
            Capability capability,
            UUID nodeId,
            CapabilityServiceStatus capabilityServiceStatus
    ) throws ConsulLoginFailedException, ResourceNotFoundException, IllegalAccessException {
        return addSingleHostCapabilityToNode(
                consulCredential,
                capability,
                nodeId,
                capabilityServiceStatus,
                false,
                new HashMap<>()
        );
    }

    public CapabilityService addSingleHostCapabilityToNode(
            ConsulCredential consulCredential,
            Capability capability,
            UUID nodeId,
            CapabilityServiceStatus capabilityServiceStatus,
            Boolean isManaged,
            Map<String, String> configParameter
    ) throws ConsulLoginFailedException, ResourceNotFoundException, IllegalAccessException {
        SingleHostCapabilityService singleHostCapabilityService = new SingleHostCapabilityService(
                capability,
                nodeId,
                capabilityServiceStatus,
                isManaged,
                capabilityUtil.getNonSecretConfigParameter(capability,configParameter)
        );
        this.consulServicesApiClient.registerServiceWithoutAccess(
                consulCredential,
                singleHostCapabilityService.getConsulNodeId(),
                singleHostCapabilityService.getService(),
                singleHostCapabilityService.getId(),
                capabilityUtil.getServicePortFromConfigParameter(capability, configParameter),
                singleHostCapabilityService.getTags(),
                singleHostCapabilityService.getServiceMeta()
        );

//        Set<ConnectionType> connectionTypes = singleHostCapabilityService.getCapability().getConnectionTypes();
//        this.consulKeyValueApiClient.createKey(
//                new ConsulCredential(),
//                serviceId+"/connectionTypes",
//                connectionTypes
//        );

        try {
            this.addReadRuleForCapabilityServiceToResourcePolicy(
                    new ConsulCredential(),
                    singleHostCapabilityService
            );
        } catch(HttpClientErrorException e) {
            LOG.error("Failed to create a read rule for service '" + singleHostCapabilityService.getService() + "' (Error msg: "+e.getMessage()+")");
        }
        var capabilityService = getCapabilityServiceOfResourceByCapabilityId(
                consulCredential,
                capability.getId(),
                nodeId
        );

        CapabilityHealthCheck healthCheck = singleHostCapabilityService.getCapability().getHealthCheck();
        if(healthCheck != null) {
            Optional<Node> optionalNode = this.consulNodesApiClient.getNodeById(
                    consulCredential,
                    nodeId
            );

            if(optionalNode.isEmpty())
                throw new ResourceNotFoundException(nodeId);

            this.consulHealthApiClient.addCheckForService(
                    consulCredential,
                    optionalNode.get().getNode(),
                    capabilityService.getId(),
                    capabilityUtil.getCheckByCapability(consulCredential, nodeId, capability)
            );
        }

        return capabilityService;
    }
    //endregion

    //region UPDATE Function
    public void updateCapabilityService(
            ConsulCredential consulCredential,
            UUID nodeId,
            CapabilityService capabilityService
    ) throws ConsulLoginFailedException {
        consulServicesApiClient.registerService(
                consulCredential,
                nodeId,
                capabilityService.getService(),
                capabilityService.getId(),
                Optional.ofNullable(capabilityService.getPort()),
                capabilityService.getTags(),
                capabilityService.getServiceMeta()
        );
    }
    //endregion

    //region DELETE Function
    public void removeCapabilityServiceFromAllConsulNodes(
            ConsulCredential consulCredential,
            Capability capability
    ) throws ConsulLoginFailedException {
        var existingResources = this.resourcesConsulClient.getResources(consulCredential);
        for (var existingResource : existingResources) {
            try {
                this.removeSingleHostCapabilityFromNode(consulCredential, capability, existingResource.getId());
            } catch (ResourceNotFoundException e) {
                LOG.warn("Unable to find resource [id = '"+existingResource.getId()+"'] => Skip removal of capability "
                        + "[id = '"+capability.getId()+"']"
                );
            }
        }
    }

    public void removeSingleHostCapabilityFromNode(
            ConsulCredential consulCredential,
            Capability capability,
            UUID nodeId
    ) throws ConsulLoginFailedException, ResourceNotFoundException {
        var nodeServices = this.consulServicesApiClient.getNodeServicesByNodeId(consulCredential, nodeId);

        Optional<NodeService> capabilityService = nodeServices.stream()
                .filter(ns -> ns.getMeta().containsKey("capabilityId"))
                .filter(ns -> ns.getMeta().get("capabilityId").equals(capability.getId().toString() ))
                .findFirst();

        if(capabilityService.isEmpty()) {
            return;
        }

//        var existingCapabilityService = new SingleHostCapabilityService(capability, nodeId);
        consulServicesApiClient.removeServiceByName(
                new ConsulCredential(),
                nodeId,
                capabilityService.get().getService()
        );
//        this.removeServiceByName(new ConsulCredential(), existingCapabilityService);
        this.removeReadRuleForCapabilityFromResourcePolicy(
                new ConsulCredential(),
                nodeId,
                capabilityService.get().getService()
        );

        var nodeChecks = this.consulHealthApiClient.getChecksOfNode(consulCredential, nodeId);
        for(var nodeCheck : nodeChecks) {
            if ( nodeCheck.getName().equals("capability_" + capability.getName())) {
                this.consulHealthApiClient.removeCheckFromNode(consulCredential, nodeId, nodeCheck.getCheckId());
            }
        }
    }
    //endregion

    //region GET Function
    public List<SingleHostCapabilityService> getSingleHostCapabilityServicesOfResource(
            ConsulCredential consulCredential,
            UUID consulNodeId
    ) throws ConsulLoginFailedException {
        List<SingleHostCapabilityService> singleHostCapabilityServices = new ArrayList<>();
        Optional<Node> optionalNode = consulNodesApiClient.getNodeById(consulCredential, consulNodeId);

        if(optionalNode.isEmpty()) {
            LOG.error("No consul node found with id = '" + consulNodeId + "'");
            return null;
        }

        Node node = optionalNode.get();

        var servicesOfNode = this.consulServicesApiClient.getNodeServices(consulCredential, node.getNode());

        for (var serviceOfNode : servicesOfNode) {
            if (serviceOfNode.getTags().contains(SingleHostCapabilityService.class.getSimpleName())) {
                Optional<Capability> capability = capabilityJpaRepository.findById(
                        UUID.fromString(serviceOfNode.getMeta().get("capabilityId"))
                );

                singleHostCapabilityServices.add(new SingleHostCapabilityService(
                    capability.get(),
                    consulNodeId,
                    UUID.fromString(serviceOfNode.getID()),
                    capabilityUtil.getStatusOfConsulService(serviceOfNode),
                    capabilityUtil.getIsManagedOfConsulService(serviceOfNode)
                ));
            }
        }

        return singleHostCapabilityServices;
    }

    public CapabilityService getCapabilityServiceForCapabilityOfResource(
            ConsulCredential consulCredential,
            Capability capability,
            UUID consulNodeId
    ) throws ConsulLoginFailedException, IllegalAccessException {

        Optional<Node> optionalNode = consulNodesApiClient.getNodeById(consulCredential, consulNodeId);

        if(optionalNode.isEmpty()) {
            LOG.error("No consul node found with id = '" + consulNodeId + "'");
            return null;
        }

        Node node = optionalNode.get();

        List<NodeService> servicesOfNode = this.consulServicesApiClient.getNodeServices(consulCredential, node.getNode());
        for (var serviceOfNode : servicesOfNode) {
            if (serviceOfNode.getTags().contains(SingleHostCapabilityService.class.getSimpleName())) {
                return new SingleHostCapabilityService(
                        capability,
                        consulNodeId,
                        UUID.fromString(serviceOfNode.getID()),
                        serviceOfNode.getPort(),
                        capabilityUtil.getStatusOfConsulService(serviceOfNode),
                        capabilityUtil.getIsManagedOfConsulService(serviceOfNode),
                        capabilityUtil.getCustomMeta(serviceOfNode)
                );
            }
        }

        return null;
    }

    public CapabilityService getCapabilityServiceOfResourceByCapabilityId(
            ConsulCredential consulCredential,
            UUID capabilityId,
            UUID consulNodeId
    ) throws ConsulLoginFailedException, IllegalAccessException {
        Optional<Capability> optionalCapability = capabilityJpaRepository.findById(capabilityId);
        Optional<Node> optionalNode = consulNodesApiClient.getNodeById(consulCredential, consulNodeId);

        if(optionalCapability.isEmpty()) {
            LOG.error("No capability found with id = '" + capabilityId + "'");
            return null;
        }

        if(optionalNode.isEmpty()) {
            LOG.error("No consul node found with id = '" + consulNodeId + "'");
            return null;
        }

        Capability capability = optionalCapability.get();
        Node node = optionalNode.get();

        var servicesOfNode = this.consulServicesApiClient.getNodeServices(consulCredential, node.getNode());

        Optional<NodeService> optionalService = servicesOfNode
                .stream()
                .filter(service -> service.getMeta().containsKey(CapabilityService.META_KEY_CAPABILITY_ID))
                .filter(nodeService ->
                        nodeService
                                .getMeta().get(CapabilityService.META_KEY_CAPABILITY_ID)
                                .equals(capabilityId.toString())
                ).findFirst();

        if(optionalService.isEmpty()) {
            LOG.error("Node with id = '" + consulNodeId + "' has no service with capabilitId = '" + capabilityId + "'");
            return null;
        }

        NodeService serviceOfNode = optionalService.get();

        return new SingleHostCapabilityService(
                capability,
                consulNodeId,
                UUID.fromString(serviceOfNode.getID()),
                serviceOfNode.getPort(),
                capabilityUtil.getStatusOfConsulService(serviceOfNode),
                capabilityUtil.getIsManagedOfConsulService(serviceOfNode),
                capabilityUtil.getCustomMeta(serviceOfNode)
        );
    }
    //endregion

    //region ACL Function
    public void addReadRuleForCapabilityServiceToResourcePolicy(
            ConsulCredential consulCredential,
            SingleHostCapabilityService singleHostCapabilityService
    ) throws ConsulLoginFailedException {
        Optional<Node> optionalNode = consulNodesApiClient.getNodeById(consulCredential, singleHostCapabilityService.getConsulNodeId());
        Node node = optionalNode.get();
        String policyName = "resource_" + node.getId();


        this.consulAclApiClient.addReadRuleToPolicy(
                consulCredential,
                policyName,
                "service",
                singleHostCapabilityService.getService()
        );

        this.consulAclApiClient.addReadRuleToPolicy(
                consulCredential,
                policyName,
                "key_prefix",
                String.valueOf(singleHostCapabilityService.getId())
        );
    }

    private void removeReadRuleForCapabilityFromResourcePolicy(
            ConsulCredential consulCredential,
            UUID nodeId,
            String serviceName
    ) throws ConsulLoginFailedException, ResourceNotFoundException {
        Optional<Node> optionalNode = consulNodesApiClient.getNodeById(consulCredential, nodeId);

        if(optionalNode.isEmpty()) {
            throw new ResourceNotFoundException(nodeId);
        }

        this.consulAclApiClient.removeReadRuleFromPolicy(
                consulCredential,
                "resource_" + optionalNode.get().getId(),
                "service",
                serviceName
        );
    }
    //endregion
}
