package com.vanatta.helene.supplies.database.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;

public class CookieUtil {

  public static void setCookie(HttpServletResponse response, String name, String value) {
    Cookie cookie = new Cookie(name, value);
    cookie.setMaxAge(14 * 24 * 60 * 60); // expires in 14 days
    cookie.setSecure(true);
    cookie.setHttpOnly(true);
    response.addCookie(cookie);
  }

  public static void deleteCookie(HttpServletResponse response, String cookieName) {
    Cookie cookie = new Cookie(cookieName, null);
    cookie.setMaxAge(0);
    cookie.setSecure(true);
    cookie.setHttpOnly(true);
    response.addCookie(cookie);
  }

  public static Optional<String> readCookieValue(HttpServletRequest request, String cookieName) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }

    return Arrays.stream(cookies)
        .filter(c -> c.getName().equals(cookieName))
        .findAny()
        .map(Cookie::getValue)
        .filter(v -> !v.isBlank());
  }
}
