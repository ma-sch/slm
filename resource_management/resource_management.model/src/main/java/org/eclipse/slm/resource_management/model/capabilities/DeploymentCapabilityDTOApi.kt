package org.eclipse.slm.resource_management.model.capabilities

import com.fasterxml.jackson.annotation.JsonTypeName
import org.eclipse.slm.common.model.DeploymentType
import org.eclipse.slm.resource_management.model.capabilities.actions.*
import java.util.*
import java.util.function.Consumer

@JsonTypeName("DeploymentCapability")
open class DeploymentCapabilityDTOApi(id: UUID? = null) : CapabilityDTOApi(id, "DeploymentCapability") {

    open var supportedDeploymentTypes: List<DeploymentType> = ArrayList()

    fun setActions(
        repo: String,
        branch: String,
        capabilityActionTypes: List<CapabilityActionType>
    ) {
        val actions = HashMap<CapabilityActionType, CapabilityAction>()

        capabilityActionTypes.forEach(
            Consumer { t: CapabilityActionType ->
                actions[t] = AwxCapabilityAction(
                    repo,
                    branch,
                    t.name + ".yml"
                )
            }
        )

        this.actions = actions
    }
}
