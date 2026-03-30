package com.qprint.auth.service;

public class Templates {
    private Templates() {}

    public static String welcomeEmail(String firstName, String code) {
        return baseWrapper("""
                <h2 style='color:#6366F1;margin-bottom:12px;'>Welcome to QPrint, %s!</h2>
                <p style='margin:0 0 12px 0;color:#e5e7eb;'>We're excited to handle your printing hustle. Verify your email to activate your account.</p>
                <p style='font-size:24px;font-weight:700;letter-spacing:4px;background:#111827;padding:12px 16px;border-radius:8px;display:inline-block;color:#fff;'>%s</p>
                <p style='margin-top:12px;color:#9ca3af;'>This code is valid for 15 minutes. If you didn't sign up, you can ignore this email.</p>
                """.formatted(firstName, code));
    }

    public static String accountCreatedEmail(String firstName) {
      return baseWrapper("""
          <h2 style='color:#22c55e;margin-bottom:12px;'>Your QPrint account is created</h2>
          <p style='color:#e5e7eb;'>Hi %s, your account has been created successfully. Please verify your email to activate it.</p>
          """.formatted(firstName));
    }

    public static String verificationCodeEmail(String code) {
        return baseWrapper("""
                <h3 style='color:#6366F1;margin-bottom:8px;'>Your verification code</h3>
                <p style='font-size:24px;font-weight:700;letter-spacing:4px;background:#111827;padding:12px 16px;border-radius:8px;display:inline-block;color:#fff;'>%s</p>
                <p style='margin-top:12px;color:#9ca3af;'>Valid for 15 minutes.</p>
                """.formatted(code));
    }

    public static String verifiedEmail(String firstName) {
        return baseWrapper("""
                <h2 style='color:#10B981;margin-bottom:12px;'>Account active 🎉</h2>
                <p style='color:#e5e7eb;'>Hi %s, your QPrint account is now active. You can log in and start printing instantly.</p>
                """.formatted(firstName));
    }

    public static String resetEmail(String firstName, String link) {
        return baseWrapper("""
                <h2 style='color:#6366F1;margin-bottom:12px;'>Reset your password</h2>
                <p style='color:#e5e7eb;'>Hi %s, click the button below to reset your password. This link expires in 1 hour.</p>
                <a href='%s' style='background:#6366F1;color:white;padding:12px 16px;border-radius:8px;text-decoration:none;font-weight:600;'>Reset Password</a>
                """.formatted(firstName, link));
    }

    public static String passwordChangedEmail(String firstName) {
        return baseWrapper("""
                <h2 style='color:#10B981;margin-bottom:12px;'>Password changed</h2>
                <p style='color:#e5e7eb;'>Hi %s, your QPrint password was reset. If this wasn't you, contact support immediately.</p>
                """.formatted(firstName));
    }

    private static String baseWrapper(String inner) {
        return """
                <div style='background:#0A0F1E;padding:24px;font-family:Inter,Helvetica,Arial,sans-serif;'>
                  <div style='max-width:640px;margin:0 auto;background:#111827;padding:24px;border-radius:12px;border:1px solid #1f2937;'>
                    <div style='font-size:18px;font-weight:700;color:#fff;margin-bottom:12px;'>QPrint</div>
                    %s
                    <p style='margin-top:24px;color:#6b7280;font-size:12px;'>QPrint · Crafted for campus convenience.</p>
                  </div>
                </div>
                """.formatted(inner);
    }
}
