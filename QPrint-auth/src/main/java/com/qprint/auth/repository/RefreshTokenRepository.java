package com.qprint.auth.repository;

import com.qprint.auth.entity.RefreshToken;
import com.qprint.auth.entity.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByUserAndTokenHash(User user, String tokenHash);
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken rt where rt.user = :user or rt.expiresAt < :now")
    void deleteByUserOrExpired(User user, Instant now);
}
