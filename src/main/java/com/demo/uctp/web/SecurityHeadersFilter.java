package com.demo.uctp.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String csp = String.join(" ",
      "default-src 'self';",
      "base-uri 'self';",
      "object-src 'none';",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval' https:;",
      "style-src 'self' 'unsafe-inline' https:;",
      "img-src 'self' data: https:;",
      "font-src 'self' data: https:;",
      "connect-src 'self' https:",
        "https://testflex.cybersource.com",
        "https://*.gcpex.visa.com",
        "https://sandbox-assets.secure.checkout.visa.com",
        "https://sandbox.auth.visa.com",
        "https://sandbox.src.mastercard.com",
        "https://*.aexp-static.com",
        "https://*.aexp.com",
        "https://*.americanexpress.com",
        "https://cdn.jsdelivr.net",
        "https://www.google-analytics.com;",
      "frame-src https:",
        "https://sandbox.auth.visa.com",
        "https://sandbox.src.mastercard.com",
        "https://sandbox-assets.secure.checkout.visa.com",
        "https://*.visa.com",
        "https://*.aexp-static.com",
        "https://*.aexp.com",
        "https://*.americanexpress.com;",
      "frame-ancestors 'self';"
    );

    response.setHeader("Content-Security-Policy", csp);

    // IMPORTANTE para tu error PaymentRequest / “payment not allowed”
    response.setHeader("Permissions-Policy", "payment=(self)");

    // Recomendados (opcionales)
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

    filterChain.doFilter(request, response);
  }
}