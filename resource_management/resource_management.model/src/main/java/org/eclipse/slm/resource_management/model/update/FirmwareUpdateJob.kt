package org.eclipse.slm.resource_management.model.update

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.eclipse.slm.common.model.AbstractBaseEntityUuid
import java.util.*

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class FirmwareUpdateJob(id: UUID? = null,
                        resourceId: UUID?,
                        softwareNameplateId: String?,
)
    : AbstractBaseEntityUuid(id) {

    @Column(name = "resource_id", nullable = false)
    var resourceId: UUID? = resourceId

    @Column(name = "software_nameplate_id", nullable = false)
    var softwareNameplateId: String? = softwareNameplateId

    @Column(name = "state", nullable = false)
    var firmwareUpdateState: FirmwareUpdateStates? = FirmwareUpdateStates.PREPARING

    protected constructor()
    : this(null, null, null)
}
