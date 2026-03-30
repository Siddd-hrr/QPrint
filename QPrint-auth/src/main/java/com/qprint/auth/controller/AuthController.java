package com.qprint.auth.controller;

import com.qprint.auth.dto.AuthDtos.*;
import com.qprint.auth.entity.User;
import com.qprint.auth.model.ApiResponse;
import com.qprint.auth.service.AuthService;
import com.qprint.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse<AuthResponse> resp = authService.register(request);
        return ResponseEntity.status(201).body(resp);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resend(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.refreshCookie() != null) builder.header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
        return builder.body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestHeader(value = "Cookie", required = false) String cookieHeader) {
        String token = CookieUtil.extract(cookieHeader, "refreshToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("No refresh token"));
        }
        var result = authService.refresh(token);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.refreshCookie() != null) builder.header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
        return builder.body(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Cookie", required = false) String cookieHeader) {
        String token = CookieUtil.extract(cookieHeader, "refreshToken");
        ApiResponse<Void> resp = authService.logout(token);
        ResponseCookie cleared = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cleared.toString()).body(resp);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> reset(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> me(HttpServletRequest request) {
        User user = currentUser(request);
        return ResponseEntity.ok(authService.me(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> profile(HttpServletRequest request,
                                                                               @Valid @RequestBody ProfileUpdateRequest body) {
        User user = currentUser(request);
        return ResponseEntity.ok(authService.updateProfile(user, body));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(HttpServletRequest request,
                                                             @Valid @RequestBody ChangePasswordRequest body) {
        User user = currentUser(request);
        return ResponseEntity.ok(authService.changePassword(user, body));
    }

    private User currentUser(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing token");
        }
        Claims claims = jwtService.parseToken(authHeader.substring(7));
        return authService.resolveUserFromClaims(claims);
    }

}

class CookieUtil {
    static String extract(String cookieHeader, String name) {
        if (cookieHeader == null) return null;
        String[] cookies = cookieHeader.split(";\\s*");
        for (String c : cookies) {
            if (c.startsWith(name + "=")) {
                return c.substring(name.length() + 1);
            }
        }
        return null;
    }
}
