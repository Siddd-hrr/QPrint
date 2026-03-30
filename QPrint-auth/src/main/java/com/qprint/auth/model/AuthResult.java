package com.qprint.auth.model;

import org.springframework.http.ResponseCookie;

public record AuthResult<T>(ApiResponse<T> response, ResponseCookie refreshCookie) {}
