package com.servicehub.repository;

import com.servicehub.model.RequestStatus;
import com.servicehub.model.ServiceRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    boolean existsByServiceIdAndStatusIn(Long serviceId, Collection<RequestStatus> statuses);

    @Query("select (count(r) > 0) from ServiceRequest r where (r.client.id = :userId or r.service.provider.id = :userId) and r.status in :statuses")
    boolean existsActiveForUser(@Param("userId") Long userId, @Param("statuses") Collection<RequestStatus> statuses);

    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    @Query("select r from ServiceRequest r where :admin = true or r.client.id = :userId or r.service.provider.id = :userId")
    Page<ServiceRequest> findVisible(@Param("userId") Long userId, @Param("admin") boolean admin, Pageable pageable);

    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    @Query("select r from ServiceRequest r where r.id = :id")
    Optional<ServiceRequest> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ServiceRequest r where r.id = :id")
    Optional<ServiceRequest> findByIdForUpdate(@Param("id") Long id);
}
