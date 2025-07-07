package org.eclipse.slm.resource_management.service.rest.resource_types;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.slm.resource_management.model.resource.ResourceType;
import org.eclipse.slm.resource_management.service.rest.resources.ResourcesManager;
import org.eclipse.slm.resource_management.service.rest.update.UpdateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/resources/types")
@Tag(name = "Resource Types")
public class ResourceTypesRestController {

    private final static Logger LOG = LoggerFactory.getLogger(ResourceTypesRestController.class);

    private final ResourceTypesManager resourceTypesManager;

    private final UpdateManager updateManager;

    @Autowired
    public ResourceTypesRestController(
            ResourcesManager resourcesManager, ResourceTypesManager resourceTypesManager, UpdateManager updateManager
    ) {
        this.resourceTypesManager = resourceTypesManager;
        this.updateManager = updateManager;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @Operation(summary="Get available resource types")
    public Collection<ResourceType> getResourceTypes() {
        return resourceTypesManager.getResourceTypes();
    }
}
