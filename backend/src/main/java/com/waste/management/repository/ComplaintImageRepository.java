package com.waste.management.repository;

import com.waste.management.entity.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, UUID> {
    List<ComplaintImage> findByComplaintId(UUID complaintId);
}
