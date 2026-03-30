package com.qprint.cart.repository;

import com.qprint.cart.model.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {
    List<CartItemEntity> findByUserId(UUID userId);
    Optional<CartItemEntity> findByUserIdAndObjectId(UUID userId, UUID objectId);
    void deleteByUserId(UUID userId);
    void deleteByUserIdAndObjectId(UUID userId, UUID objectId);
}
