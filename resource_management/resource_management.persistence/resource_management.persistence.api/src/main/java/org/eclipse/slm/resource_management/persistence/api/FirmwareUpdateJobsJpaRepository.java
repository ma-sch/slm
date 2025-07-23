package org.eclipse.slm.resource_management.persistence.api;

import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FirmwareUpdateJobsJpaRepository extends JpaRepository<FirmwareUpdateJob, UUID> {

        List<FirmwareUpdateJob> findByResourceId(UUID resourceId);

}
