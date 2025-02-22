package org.eclipse.slm.service_management.service.rest.service_instances;

import org.eclipse.slm.common.consul.client.ConsulCredential;
import org.eclipse.slm.common.consul.client.apis.ConsulServicesApiClient;
import org.eclipse.slm.common.consul.model.catalog.ConsulService;
import org.eclipse.slm.common.consul.model.exceptions.ConsulLoginFailedException;
import org.eclipse.slm.service_management.model.services.ServiceInstance;
import org.eclipse.slm.service_management.model.services.exceptions.ServiceInstanceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ServiceInstancesConsulClient {

    private final static Logger LOG = LoggerFactory.getLogger(ServiceInstancesConsulClient.class);

    private final ConsulServicesApiClient consulServicesApiClient;

    public ServiceInstancesConsulClient(ConsulServicesApiClient consulServicesApiClient) {
        this.consulServicesApiClient = consulServicesApiClient;
    }

    public void registerConsulServiceForServiceInstance(ServiceInstance serviceInstance) {
        try {
            var consulService = this.convertServiceInstanceToConsulService(serviceInstance);
            this.consulServicesApiClient.registerServiceForNodeWithReadAccessViaKeycloakRole(
                    new ConsulCredential(),
                    consulService.getNodeId(),
                    consulService.getServiceName(),
                    consulService.getServiceId(),
                    Optional.empty(),
                    consulService.getServiceTags(),
                    consulService.getServiceMetaData()
            );
        } catch (ConsulLoginFailedException e) {
            LOG.error(e.getMessage());
        }
    }

    public void updateConsulServiceForServiceInstance(ServiceInstance serviceInstance) {
        try {
            var consulService = this.convertServiceInstanceToConsulService(serviceInstance);
            this.consulServicesApiClient.registerService(
                    new ConsulCredential(),
                    consulService.getNodeId(),
                    consulService.getServiceName(),
                    consulService.getServiceId(),
                    Optional.empty(),
                    consulService.getServiceTags(),
                    consulService.getServiceMetaData()
            );
        } catch (ConsulLoginFailedException e) {
            LOG.error(e.getMessage());
        }
    }

    public ServiceInstance getServiceInstance(UUID serviceInstanceId) throws ConsulLoginFailedException, ServiceInstanceNotFoundException {
        var consulServiceOptional = this.consulServicesApiClient.getServiceById(new ConsulCredential(), serviceInstanceId);
        if (consulServiceOptional.isPresent()) {
            var serviceInstnace = ServiceInstance.Companion.ofMetaDataAndTags(
                    consulServiceOptional.get().getServiceMeta(),
                    consulServiceOptional.get().getServiceTags()
            );
            return (serviceInstnace);
        }
        throw new ServiceInstanceNotFoundException(serviceInstanceId);
    }

    public void deregisterConsulServiceForServiceInstance(ServiceInstance serviceInstance) {
        try {
            var consulServiceName = this.getConsulServiceNameForServiceInstance(serviceInstance.getId());
            this.consulServicesApiClient.removeServiceAndAclByName(new ConsulCredential(), serviceInstance.getResourceId(), consulServiceName);
        } catch (ConsulLoginFailedException e) {
            LOG.error("Error login in to Consul to remove undeployed service '" + serviceInstance.getId() + "'");
        }
    }

    public String getConsulServiceNameForServiceInstance(UUID serviceInstanceId) {
        return "service_" + serviceInstanceId;
    }

    public String getConsulServiceNameForServiceInstance(ServiceInstance serviceInstance) {
        return this.getConsulServiceNameForServiceInstance(serviceInstance.getId());
    }

    public ConsulService convertServiceInstanceToConsulService(ServiceInstance serviceInstance) {
        var consulService = new ConsulService(
                serviceInstance.getResourceId(),
                this.getConsulServiceNameForServiceInstance(serviceInstance),
                serviceInstance.getId(),
                serviceInstance.getTags(),
                serviceInstance.getMetaData()
        );

        return consulService;
    }
}
