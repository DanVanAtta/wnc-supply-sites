package com.vanatta.helene.supplies.database.auth;

import com.vanatta.helene.supplies.database.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Controller;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads 'user' cookie and puts the value into MDC. This allows any further logging messages to
 * automatically include this 'user' value.
 */
@Controller
public class UserMdcLoggingInterceptor extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CookieUtil.readUserCookie(request).ifPresent(user -> MDC.put("user", user));
    MDC.put("requestId", UUID.randomUUID().toString().substring(0, 5));
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("user");
      MDC.remove("requestId");
    }
  }
}
