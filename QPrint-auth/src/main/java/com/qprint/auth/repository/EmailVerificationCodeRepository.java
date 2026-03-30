package com.qprint.auth.repository;

import com.qprint.auth.entity.EmailVerificationCode;
import com.qprint.auth.entity.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {
    Optional<EmailVerificationCode> findTopByUserOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("delete from EmailVerificationCode ev where ev.user = :user and ev.used = false")
    void invalidateUnused(User user);

    @Modifying
    @Query("delete from EmailVerificationCode ev where ev.expiresAt < :now")
    void deleteExpired(Instant now);
}
