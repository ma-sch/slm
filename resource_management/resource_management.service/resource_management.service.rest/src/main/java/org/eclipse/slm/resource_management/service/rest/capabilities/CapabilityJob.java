package org.eclipse.slm.resource_management.service.rest.capabilities;

import org.eclipse.slm.common.awx.client.AwxCredential;
import org.eclipse.slm.common.consul.client.ConsulCredential;
import org.eclipse.slm.resource_management.model.consul.capability.CapabilityService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public class CapabilityJob {
    private UUID resourceId;
    private AwxCredential awxCredential;

    private JwtAuthenticationToken jwtAuthenticationToken;

    protected ConsulCredential consulCredential;
    private CapabilityService capabilityService;

    public CapabilityJob(UUID resourceId, CapabilityService capabilityService) {
        this.resourceId = resourceId;
        this.capabilityService = capabilityService;
    }

    public CapabilityJob(UUID resourceId, AwxCredential awxCredential, ConsulCredential consulCredential, CapabilityService capabilityService, JwtAuthenticationToken jwtAuthenticationToken) {
        this.resourceId = resourceId;
        this.awxCredential = awxCredential;
        this.consulCredential = consulCredential;
        this.capabilityService = capabilityService;
        this.jwtAuthenticationToken = jwtAuthenticationToken;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public CapabilityService getCapabilityService() {
        return capabilityService;
    }

    public JwtAuthenticationToken getJwtAuthenticationToken() {
        return jwtAuthenticationToken;
    }
}
