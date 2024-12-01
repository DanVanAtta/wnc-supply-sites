package com.vanatta.helene.supplies.database.incoming.webhook;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Slf4j
class WebhookSecret {
  private static String secret;

  WebhookSecret(Jdbi jdbi) {
    String query = "select secret from webhook_auth_secret";
    secret = jdbi.withHandle(handle -> handle.createQuery(query).mapTo(String.class).one());
  }

  boolean isValid(String value) {
    if (secret.equals(value)) {
      return true;
    } else {
      log.warn("Invalid webhook secret was attempted: {}", value);
      return false;
    }
  }
}