package org.eclipse.slm.resource_management.model.update

import java.time.ZonedDateTime

class FirmwareUpdateFile(

    var fileName: String,

    var fileSizeBytes: Long,

    var uploadDate: ZonedDateTime,
)
{
    var downloadUrl: String = ""
}