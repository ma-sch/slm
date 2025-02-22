package org.eclipse.slm.resource_management.service.rest.handler;

import org.eclipse.slm.common.awx.client.AwxCredential;
import org.eclipse.slm.common.awx.client.observer.AwxJobExecutor;
import org.eclipse.slm.common.awx.client.observer.AwxJobObserver;
import org.eclipse.slm.common.awx.client.observer.AwxJobObserverInitializer;
import org.eclipse.slm.common.awx.client.observer.IAwxJobObserverListener;
import org.eclipse.slm.common.awx.model.ExtraVars;
import org.eclipse.slm.common.consul.client.ConsulCredential;
import org.eclipse.slm.common.consul.client.apis.ConsulAclApiClient;
import org.eclipse.slm.common.consul.client.apis.ConsulNodesApiClient;
import org.eclipse.slm.common.consul.client.apis.ConsulServicesApiClient;
import org.eclipse.slm.common.consul.model.exceptions.ConsulLoginFailedException;
import org.eclipse.slm.common.keycloak.config.KeycloakUtil;
import org.eclipse.slm.common.keycloak.config.MultiTenantKeycloakRegistration;
import org.eclipse.slm.common.vault.client.VaultClient;
import org.eclipse.slm.common.vault.client.VaultCredential;
import org.eclipse.slm.common.vault.model.KvPath;
import org.eclipse.slm.notification_service.model.*;
import org.eclipse.slm.notification_service.service.client.NotificationServiceClient;
import org.eclipse.slm.resource_management.service.rest.capabilities.CapabilitiesConsulClient;
import org.eclipse.slm.resource_management.service.rest.capabilities.MultiHostCapabilitiesConsulClient;
import org.eclipse.slm.resource_management.model.capabilities.actions.AwxCapabilityAction;
import org.eclipse.slm.resource_management.model.capabilities.actions.CapabilityActionType;
import org.eclipse.slm.resource_management.model.consul.capability.CapabilityServiceStatus;
import org.eclipse.slm.resource_management.model.consul.capability.MultiHostCapabilityService;
import org.eclipse.slm.resource_management.service.rest.handler.ClusterJob;
import org.keycloak.KeycloakPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.util.*;

@Component
class ClusterDeleteFunctions extends AbstractClusterFunctions implements IAwxJobObserverListener {

    private final static Logger LOG = LoggerFactory.getLogger(ClusterDeleteFunctions.class);

    public ClusterDeleteFunctions(
            NotificationServiceClient notificationServiceClient,
            AwxJobExecutor awxJobExecutor,
            MultiTenantKeycloakRegistration multiTenantKeycloakRegistration,
            ConsulServicesApiClient consulServicesApiClient,
            ConsulAclApiClient consulAclApiClient,
            ConsulNodesApiClient consulNodesApiClient,
            CapabilitiesConsulClient capabilitiesConsulClient,
            MultiHostCapabilitiesConsulClient multiHostCapabilitiesConsulClient,
            KeycloakUtil keycloakUtil,
            AwxJobObserverInitializer awxJobObserverInitializer,
            VaultClient vaultClient) {
        super(
                notificationServiceClient,
                awxJobExecutor,
                multiTenantKeycloakRegistration,
                consulServicesApiClient,
                consulAclApiClient,
                consulNodesApiClient,
                capabilitiesConsulClient,
                multiHostCapabilitiesConsulClient,
                keycloakUtil,
                awxJobObserverInitializer,
                vaultClient);
    }

    public ClusterJob createClusterJob(KeycloakPrincipal keycloakPrincipal, MultiHostCapabilityService multiHostCapabilityService
    ) throws SSLException {
        var clusterJob = new ClusterJob(multiHostCapabilityService);

        AwxCapabilityAction uninstallAction =
                (AwxCapabilityAction) multiHostCapabilityService.getCapability().getActions().get(CapabilityActionType.UNINSTALL);

        Map<String, Object> extraVarsMap = new HashMap<>();
        extraVarsMap.put("resource_id",multiHostCapabilityService.getId().toString());
        extraVarsMap.put("keycloak_token", keycloakPrincipal.getKeycloakSecurityContext().getTokenString());
        extraVarsMap.put("service_name", multiHostCapabilityService.getService());
        extraVarsMap.put("supported_connection_types", uninstallAction.getConnectionTypes());
        ExtraVars extraVars = new ExtraVars(extraVarsMap);

        var jobId = awxJobExecutor.executeJob(
                new AwxCredential(keycloakPrincipal),
                uninstallAction.getAwxRepo(),
                uninstallAction.getAwxBranch(),
                uninstallAction.getPlaybook(),
                extraVars
        );
        var awxJobObserver = this.awxJobObserverInitializer.init(jobId, JobTarget.RESOURCE, JobGoal.DELETE, this);
        clusterJob.setAwxJobObserver(awxJobObserver);

        return clusterJob;
    }

    private void delete(KeycloakPrincipal keycloakPrincipal, MultiHostCapabilityService multiHostCapabilityService
    ) throws SSLException, ConsulLoginFailedException {
        var clusterJob = createClusterJob(
                keycloakPrincipal,
                multiHostCapabilityService
        );

        clusterJob.setKeycloakPrincipal(keycloakPrincipal);
        this.clusterJobMap.put(clusterJob.getAwxJobObserver(), clusterJob);
        multiHostCapabilityService.setStatus(CapabilityServiceStatus.UNINSTALL);
        multiHostCapabilitiesConsulClient.updateMultiHostCapabilityService(
                new ConsulCredential(),
                multiHostCapabilityService
        );

        this.notificationServiceClient.postJobObserver(keycloakPrincipal, clusterJob.getAwxJobObserver());
    }

    public void delete(KeycloakPrincipal keycloakPrincipal, UUID consulServiceUuid
    ) throws SSLException, ConsulLoginFailedException {
        Optional<MultiHostCapabilityService> service = multiHostCapabilitiesConsulClient.getMultiHostCapabilityServiceOfUser(
                new ConsulCredential(keycloakPrincipal),
                consulServiceUuid
        );

        if(service.isPresent()) {
            this.delete(keycloakPrincipal, service.get());
        }
    }

    @Override
    public void onJobStateChanged(AwxJobObserver sender, JobState newState) { }

    @Override
    public void onJobStateFinished(AwxJobObserver sender, JobFinalState finalState) {
        LOG.info("Job on cluster finished.");
        var jobGoal = sender.jobGoal;
        var jobTarget = sender.jobTarget;
        var clusterJob = this.clusterJobMap.get(sender);
        var keycloakPrincipal = clusterJob.getKeycloakPrincipal();
        var multiHostCapabilityService = clusterJob.getMultiHostCapabilityService();

        if(!finalState.equals(JobFinalState.SUCCESSFUL)) {
            LOG.warn("Job with id='" + sender.jobId + "' finished not successful ('" + finalState.name().toString() + "')");
            this.clusterJobMap.remove(sender.jobId);
            return;
        }

        if (jobGoal.equals(JobGoal.DELETE)) {
            // Delete Keycloak role
            this.keycloakUtil.deleteRealmRole(keycloakPrincipal, multiHostCapabilityService.getService());
            this.keycloakUtil.deleteRealmRole(keycloakPrincipal, "resource_" + multiHostCapabilityService.getId());

            // Remove read access for secret of awx policy
            String resourceId = multiHostCapabilityService.getId().toString();
            VaultCredential vaultCredential = new VaultCredential();
            this.vaultClient.removeRuleFromPolicy(
                    vaultCredential,
                    "awx",
                    "resources/data/"+ resourceId
            );

            // Delete secret from KV engine
            KvPath resourceVaultPath = new KvPath("resources", resourceId);
            this.vaultClient.removeSecretFromKvEngine(
                    vaultCredential,
                    resourceVaultPath.getSecretEngine(),
                    resourceVaultPath.getPath()
            );

            // remove policy
            this.vaultClient.removePolicy(
                    vaultCredential,
                    "policy_resource_" + resourceId
            );
            // remove group
            this.vaultClient.removeGroup(
                    vaultCredential,
                    "group_resource_" + resourceId
            );

            // Delete cluster representation in consul
            try {
                multiHostCapabilitiesConsulClient.removeMultiHostCapabilityService(
                        new ConsulCredential(),
                        multiHostCapabilityService.getId()
                );
            } catch (ConsulLoginFailedException e) {
                LOG.error("Failed to delete MultiHostCapabilityService [id = '"+multiHostCapabilityService.getId()+"'] due to login error");
            }

            this.notificationServiceClient.postNotification(
                    clusterJob.getKeycloakPrincipal(),
                    Category.Resources,
                    jobTarget,
                    jobGoal
            );
            this.clusterJobMap.remove(sender.jobId);
        }
    }
}
