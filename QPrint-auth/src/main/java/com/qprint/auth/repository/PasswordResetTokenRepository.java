package com.qprint.auth.repository;

import com.qprint.auth.entity.PasswordResetToken;
import com.qprint.auth.entity.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from PasswordResetToken prt where prt.user = :user or prt.expiresAt < :now")
    void deleteByUserOrExpired(User user, Instant now);
}
