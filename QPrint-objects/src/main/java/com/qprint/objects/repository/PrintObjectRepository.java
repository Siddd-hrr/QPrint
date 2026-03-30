package com.qprint.objects.repository;

import com.qprint.objects.model.PrintObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrintObjectRepository extends JpaRepository<PrintObject, UUID> {
    Optional<PrintObject> findByIdAndUserId(UUID id, UUID userId);
}
