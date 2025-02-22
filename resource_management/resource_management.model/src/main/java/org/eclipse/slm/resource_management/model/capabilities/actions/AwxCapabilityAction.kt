package org.eclipse.slm.resource_management.model.capabilities.actions

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeName
import org.eclipse.slm.common.awx.model.SurveyItem
import org.eclipse.slm.resource_management.model.resource.ConnectionType

@JsonTypeName("AwxCapabilityAction")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class AwxCapabilityAction(): CapabilityAction(AwxCapabilityAction::class.java.simpleName) {

    var awxRepo: String = ""

    var awxBranch: String = ""

    var playbook: String = ""

    @JsonIgnore
    var username: String = ""

    @JsonIgnore
    var password: String = ""

    var parameter: List<SurveyItem>? = null;

    constructor(
         awxRepo: String,
         awxBranch: String,
         playbook: String
    ) : this() {
        this.capabilityActionType = capabilityActionType
        this.awxRepo = awxRepo
        this.awxBranch = awxBranch
        this.playbook = playbook
    }

    constructor(
        awxRepo: String,
        awxBranch: String,
        playbook: String,
        connectionTypes: HashSet<ConnectionType>
    ) : this(awxRepo, awxBranch, playbook) {
        super.connectionTypes = connectionTypes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AwxCapabilityAction) return false

        if (awxRepo != other.awxRepo) return false
        if (awxBranch != other.awxBranch) return false
        if (playbook != other.playbook) return false

        return true
    }

    override fun hashCode(): Int {
        var result = awxRepo.hashCode()
        result = 31 * result + awxBranch.hashCode()
        result = 31 * result + playbook.hashCode()
        return result
    }

}
