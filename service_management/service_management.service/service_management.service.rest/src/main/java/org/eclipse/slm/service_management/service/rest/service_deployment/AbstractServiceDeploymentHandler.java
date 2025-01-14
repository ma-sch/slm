package org.eclipse.slm.service_management.service.rest.service_deployment;

import org.eclipse.slm.common.awx.client.AwxCredential;
import org.eclipse.slm.common.awx.client.observer.AwxJobExecutor;
import org.eclipse.slm.common.awx.client.observer.AwxJobObserver;
import org.eclipse.slm.common.awx.client.observer.AwxJobObserverInitializer;
import org.eclipse.slm.common.awx.client.observer.IAwxJobObserverListener;
import org.eclipse.slm.common.awx.model.ExtraVars;
import org.eclipse.slm.notification_service.model.JobGoal;
import org.eclipse.slm.notification_service.model.JobTarget;
import org.eclipse.slm.resource_management.model.capabilities.Capability;
import org.eclipse.slm.resource_management.model.capabilities.actions.AwxCapabilityAction;
import org.eclipse.slm.resource_management.model.capabilities.actions.CapabilityActionType;
import org.eclipse.slm.resource_management.model.capabilities.provider.ServiceHoster;
import org.eclipse.slm.resource_management.model.capabilities.provider.ServiceHosterFilter;
import org.eclipse.slm.resource_management.service.client.ResourceManagementApiClientInitializer;
import org.eclipse.slm.resource_management.service.client.handler.*;
import org.eclipse.slm.service_management.service.rest.service_instances.ServiceInstancesConsulClient;
import org.keycloak.KeycloakPrincipal;

import javax.net.ssl.SSLException;
import java.util.UUID;

public class AbstractServiceDeploymentHandler {

    protected final ResourceManagementApiClientInitializer resourceManagementApiClientInitializer;

    protected final ServiceInstancesConsulClient serviceInstancesConsulClient;

    protected final AwxJobObserverInitializer awxJobObserverInitializer;

    protected final AwxJobExecutor awxJobExecutor;

    public AbstractServiceDeploymentHandler(ResourceManagementApiClientInitializer resourceManagementApiClientInitializer, ServiceInstancesConsulClient serviceInstancesConsulClient, AwxJobObserverInitializer awxJobObserverInitializer, AwxJobExecutor awxJobExecutor) {
        this.resourceManagementApiClientInitializer = resourceManagementApiClientInitializer;
        this.serviceInstancesConsulClient = serviceInstancesConsulClient;
        this.awxJobObserverInitializer = awxJobObserverInitializer;
        this.awxJobExecutor = awxJobExecutor;
    }

    protected AwxCapabilityAction getAwxDeployCapabilityAction(CapabilityActionType actionType, Capability capability) {
        if (capability.getActions().containsKey(actionType)) {
            var capabilityAction = capability.getActions().get(actionType);

            if (capabilityAction instanceof AwxCapabilityAction) {
                var awxCapabilityAction = (AwxCapabilityAction) capabilityAction;
                return awxCapabilityAction;
            }
            else {
                throw new RuntimeException("'CapabilityActionType.deploy' of Deployment Capability '"
                        + capability.getName() + "' needs to be of type 'AwxCapabilityAction'");
            }
        }
        else {
            throw new RuntimeException("Deployment Capability '" + capability.getName() + "' has no 'CapabilityActionType.deploy'");
        }
    }

    protected AwxJobObserver runAwxCapabilityAction(AwxCapabilityAction awxCapabilityAction,
                                                    KeycloakPrincipal keycloakPrincipal,
                                                    ExtraVars extraVars,
                                                    JobGoal jobGoal,
                                                    IAwxJobObserverListener listner)
            throws SSLException {
        var jobTarget = JobTarget.SERVICE;
        var awxJobId = awxJobExecutor.executeJob(
                new AwxCredential(keycloakPrincipal),
                awxCapabilityAction.getAwxRepo(), awxCapabilityAction.getAwxBranch(), awxCapabilityAction.getPlaybook(),
                extraVars);
        var awxJobObserver = awxJobObserverInitializer.init(awxJobId, jobTarget, jobGoal, listner);

        return awxJobObserver;
    }

    protected ServiceHoster getServiceHoster(
            KeycloakPrincipal keycloakPrincipal, UUID deploymentCapabilityServiceId)
            throws SSLException, ApiException, CapabilityServiceNotFoundException, ApiException {
        var resourceManagementApiClient = resourceManagementApiClientInitializer.init(keycloakPrincipal);
        var capabilityProvidersRestControllerApi = new CapabilityProvidersRestControllerApi(resourceManagementApiClient);

        var serviceHosterFilter = new ServiceHosterFilter.Builder()
                .capabilityServiceId(deploymentCapabilityServiceId)
                .build();
        var serviceHosters = capabilityProvidersRestControllerApi.getServiceHosters("fabos", serviceHosterFilter);

        if (serviceHosters.size() > 0) {
            return serviceHosters.get(0);
        }

        throw new CapabilityServiceNotFoundException(deploymentCapabilityServiceId);
    }
}
