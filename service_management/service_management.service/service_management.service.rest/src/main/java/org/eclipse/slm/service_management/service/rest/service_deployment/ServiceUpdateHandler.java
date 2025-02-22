package org.eclipse.slm.service_management.service.rest.service_deployment;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.slm.common.awx.client.observer.AwxJobExecutor;
import org.eclipse.slm.common.awx.client.observer.AwxJobObserver;
import org.eclipse.slm.common.awx.client.observer.AwxJobObserverInitializer;
import org.eclipse.slm.common.awx.client.observer.IAwxJobObserverListener;
import org.eclipse.slm.common.awx.model.ExtraVars;
import org.eclipse.slm.common.consul.client.apis.ConsulServicesApiClient;
import org.eclipse.slm.common.consul.model.exceptions.ConsulLoginFailedException;
import org.eclipse.slm.common.keycloak.config.KeycloakUtil;
import org.eclipse.slm.common.utils.keycloak.KeycloakTokenUtil;
import org.eclipse.slm.notification_service.model.*;
import org.eclipse.slm.notification_service.service.client.NotificationServiceClient;
import org.eclipse.slm.resource_management.model.capabilities.DeploymentCapability;
import org.eclipse.slm.resource_management.model.capabilities.actions.CapabilityActionType;
import org.eclipse.slm.resource_management.service.client.ResourceManagementApiClientInitializer;
import org.eclipse.slm.resource_management.service.client.handler.ApiException;
import org.eclipse.slm.service_management.model.exceptions.ServiceOptionNotFoundException;
import org.eclipse.slm.service_management.service.rest.utils.DockerContainerServiceOfferingOrderUtil;
import org.eclipse.slm.service_management.model.offerings.ServiceOrder;
import org.eclipse.slm.service_management.model.offerings.ServiceOfferingVersion;
import org.eclipse.slm.service_management.model.offerings.ServiceOrderResult;
import org.eclipse.slm.service_management.model.offerings.exceptions.InvalidServiceOfferingDefinitionException;
import org.eclipse.slm.service_management.model.services.ServiceInstance;
import org.eclipse.slm.service_management.model.services.exceptions.ServiceInstanceNotFoundException;
import org.eclipse.slm.service_management.persistence.api.ServiceOrderJpaRepository;
import org.eclipse.slm.service_management.service.rest.docker_compose.DockerComposeFile;
import org.eclipse.slm.service_management.service.rest.docker_compose.DockerComposeFileParser;
import org.eclipse.slm.service_management.service.rest.service_deployment.UpdateJobRun;
import org.eclipse.slm.service_management.service.rest.service_instances.ServiceInstancesConsulClient;
import org.apache.commons.lang3.NotImplementedException;
import org.keycloak.KeycloakPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ServiceUpdateHandler extends AbstractServiceDeploymentHandler implements IAwxJobObserverListener {

    public final static Logger LOG = LoggerFactory.getLogger(ServiceUpdateHandler.class);

    private final NotificationServiceClient notificationServiceClient;

    private final ConsulServicesApiClient consulServicesApiClient;

    private final KeycloakUtil keycloakUtil;

    private final ServiceOrderJpaRepository serviceOrderJpaRepository;

    private Map<AwxJobObserver, UpdateJobRun> observedAwxJobsToUpdateJobDetails = new HashMap<>();

    public ServiceUpdateHandler(AwxJobObserverInitializer awxJobObserverInitializer,
                                AwxJobExecutor awxJobExecutor,
                                NotificationServiceClient notificationServiceClient,
                                ConsulServicesApiClient consulServicesApiClient,
                                KeycloakUtil keycloakUtil,
                                ResourceManagementApiClientInitializer resourceManagementApiClientInitializer,
                                ServiceOrderJpaRepository serviceOrderJpaRepository,
                                ServiceInstancesConsulClient serviceInstancesConsulClient) {
        super(resourceManagementApiClientInitializer, serviceInstancesConsulClient, awxJobObserverInitializer, awxJobExecutor);
        this.notificationServiceClient = notificationServiceClient;
        this.consulServicesApiClient = consulServicesApiClient;
        this.keycloakUtil = keycloakUtil;
        this.serviceOrderJpaRepository = serviceOrderJpaRepository;
    }

    public UpdateJobRun updateServiceInstance(
            KeycloakPrincipal keycloakPrincipal,
            ServiceInstance serviceInstance,
            ServiceOfferingVersion serviceOfferingVersion)
            throws SSLException, JsonProcessingException, ServiceOptionNotFoundException, ApiException, InvalidServiceOfferingDefinitionException, CapabilityServiceNotFoundException {

        var serviceOrders = serviceOrderJpaRepository.findByServiceInstanceId(serviceInstance.getId());
        var successfulServiceOrdersSortedDescendingCreated = serviceOrders.stream()
                .filter(so -> so.getServiceOrderResult().equals(ServiceOrderResult.SUCCESSFULL))
                .sorted(Comparator.comparing(ServiceOrder::getCreated).reversed())
                .collect(Collectors.toList());
        var latestServiceOrder = successfulServiceOrdersSortedDescendingCreated.get(0);

        var serviceHoster = this.getServiceHoster(keycloakPrincipal, latestServiceOrder.getDeploymentCapabilityServiceId());
        var deploymentCapability = (DeploymentCapability)serviceHoster.getCapabilityService().getCapability();

        switch (serviceOfferingVersion.getDeploymentType()) {
            case DOCKER_CONTAINER:
            case DOCKER_COMPOSE: {
                var deployableComposeFile = this.getDeployableComposeFile(serviceOfferingVersion, latestServiceOrder);
                var awxCapabilityAction = this.getAwxDeployCapabilityAction(CapabilityActionType.UPDATE, deploymentCapability);

                HashMap<String, Object> extraVarsMap = new HashMap<>() {{
                    put("keycloak_token", keycloakPrincipal.getKeycloakSecurityContext().getTokenString());
                    put("resource_id", serviceInstance.getResourceId());
                    put("service_id", serviceInstance.getId());
                    put("service_name", serviceHoster.getCapabilityService().getService());
                    put("supported_connection_types", awxCapabilityAction.getConnectionTypes());
                    put("docker_compose_file", deployableComposeFile);
                }};
                extraVarsMap = this.addExtraVarsForServiceRepositories(extraVarsMap, serviceOfferingVersion);
                var extraVars = new ExtraVars(extraVarsMap);

                var awxJobObserver = this.runAwxCapabilityAction(awxCapabilityAction, keycloakPrincipal, extraVars, JobGoal.UPDATE, this);
                this.notificationServiceClient.postJobObserver(keycloakPrincipal, awxJobObserver);

                var updateServiceOrder = new ServiceOrder();
                updateServiceOrder.setServiceInstanceId(latestServiceOrder.getServiceInstanceId());
                updateServiceOrder.setDeploymentCapabilityServiceId(latestServiceOrder.getDeploymentCapabilityServiceId());
                updateServiceOrder.setServiceOptionValues(latestServiceOrder.getServiceOptionValues());
                var updateJobRun = new UpdateJobRun(
                        awxJobObserver, keycloakPrincipal, serviceInstance.getId(), updateServiceOrder,
                        serviceInstance.getResourceId(), serviceOfferingVersion);
                this.observedAwxJobsToUpdateJobDetails.put(awxJobObserver, updateJobRun);

                return updateJobRun;
            }

            default:
                throw new NotImplementedException("Update for deployment type '" + serviceOfferingVersion.getDeploymentType() + "' not supported");
        }
    }

    private HashMap<String, Object> addExtraVarsForServiceRepositories(HashMap<String, Object> extraVarsMap, ServiceOfferingVersion serviceOfferingVersion) {
        if (serviceOfferingVersion.getServiceRepositories().size() > 0) {
            var dockerRegistriesVaultPaths = new ArrayList<String>();
            for (var serviceRepositoryId : serviceOfferingVersion.getServiceRepositories()) {
                dockerRegistriesVaultPaths.add("vendor_" + serviceOfferingVersion.getServiceOffering().getServiceVendor().getId() + "/" + serviceRepositoryId);
            }
            extraVarsMap.put("docker_registries_vault_paths", dockerRegistriesVaultPaths);
        }

        return extraVarsMap;
    }

    private DockerComposeFile getDeployableComposeFile(ServiceOfferingVersion serviceOfferingVersion, ServiceOrder serviceOrder)
            throws JsonProcessingException, ServiceOptionNotFoundException, InvalidServiceOfferingDefinitionException {
        DockerComposeFile deployableComposeFile = null;
        switch (serviceOfferingVersion.getDeploymentType())
        {
            case DOCKER_CONTAINER:
                deployableComposeFile = DockerContainerServiceOfferingOrderUtil
                        .generateDockerComposeFile(serviceOfferingVersion, serviceOrder);
                break;

            case DOCKER_COMPOSE:
                deployableComposeFile = DockerComposeFileParser.generateDeployableComposeFileForServiceOffering(
                        serviceOfferingVersion, serviceOrder.getServiceOptionValues());
                break;
        }

        if (deployableComposeFile == null) {
            throw new RuntimeException("Unable to create deployable Docker Compose File for order of service offering '"
                    + serviceOfferingVersion.getId() + "' version '" + serviceOfferingVersion.getVersion() + "'");
        }
        else {
            return deployableComposeFile;
        }
    }

    @Override
    public void onJobStateChanged(AwxJobObserver sender, JobState newState) {
    }

    @Override
    public void onJobStateFinished(AwxJobObserver sender, JobFinalState finalState) {
        if (this.observedAwxJobsToUpdateJobDetails.containsKey(sender))
        {
            var jobDetails = this.observedAwxJobsToUpdateJobDetails.get(sender);
            var keycloakPrincipal = jobDetails.getKeycloakPrincipal();
            var userUuid = KeycloakTokenUtil.getUserUuid(keycloakPrincipal);
            var serviceInstanceId = jobDetails.getServiceInstanceId();
            var serviceOfferingVersion = jobDetails.getServiceOfferingVersion();
            var serviceOrder = jobDetails.getServiceOrder();
            if (finalState == JobFinalState.SUCCESSFUL)
            {
                // Update consul service of service instance
                try {
                    var serviceInstance = this.serviceInstancesConsulClient.getServiceInstance(jobDetails.getServiceInstanceId());
                    serviceInstance.setServiceOfferingVersionId(jobDetails.getServiceOfferingVersion().getId());
                    this.serviceInstancesConsulClient.updateConsulServiceForServiceInstance(serviceInstance);

                    LOG.info("Service '" + serviceInstanceId + "' update for user '" + userUuid + "' to version '" + serviceOfferingVersion.getVersion() + "' successful");

                    serviceOrder.setServiceOrderResult(ServiceOrderResult.SUCCESSFULL);
                    notificationServiceClient.postNotification(keycloakPrincipal, Category.Services, JobTarget.SERVICE, JobGoal.UPDATE);
                } catch (ConsulLoginFailedException | ServiceInstanceNotFoundException e) {
                    LOG.error(e.getMessage());
                }
            }
            else {
                serviceOrder.setServiceOrderResult(ServiceOrderResult.FAILED);
                LOG.info("Service '" + serviceInstanceId + "'not updated to version '" + serviceOfferingVersion.getVersion()
                        + "' for user '" + userUuid + "', because job '" + sender.jobId +"' " + finalState);
            }

            this.serviceOrderJpaRepository.save(serviceOrder);
            this.observedAwxJobsToUpdateJobDetails.remove(sender);
        }
    }
}
