package com.commerceflow.catalog.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductIdAndStatus(UUID productId, String status, Pageable pageable);
    Page<Review> findByStatus(String status, Pageable pageable);
}
