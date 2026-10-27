package com.servicehub.repository;

import com.servicehub.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByRequestId(Long requestId);

    @EntityGraph(attributePaths = {"request", "request.service"})
    @Query("select r from Review r where r.request.service.id = :serviceId")
    Page<Review> findByServiceId(@Param("serviceId") Long serviceId, Pageable pageable);
}
