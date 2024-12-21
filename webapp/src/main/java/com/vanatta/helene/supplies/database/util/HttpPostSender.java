package com.vanatta.helene.supplies.database.util;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/** Utility class for sending HTTP POST messages. */
@Slf4j
public class HttpPostSender {

  public static void sendAsJson(String url, Object toSend) {
    String message = new Gson().toJson(toSend);
    sendJson(url, message);
  }

  /** Sends a string message already formatted as a JSON. */
  public static void sendJson(String url, String json) {
    if (!url.startsWith("http")) {
      throw new IllegalArgumentException("Invalid url: " + url);
    }
    log.info("Sending to url: {}, JSON: {}", url, json);

    try (var client = HttpClient.newHttpClient()) {
      var uri = URI.create(url);
      var request =
          HttpRequest.newBuilder(uri)
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .header("Content-type", "application/json")
              .build();

      try {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          log.info("Successfully sent!");
        } else {
          log.error("Failed, bad response received: {}, {}", response, response.body());
        }
      } catch (IOException | InterruptedException e) {
        log.error("Failed to send data to URL: {}, data: {}", url, json, e);
        throw new RuntimeException(e);
      }
    }
  }
}
