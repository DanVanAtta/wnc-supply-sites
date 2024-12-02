package com.vanatta.helene.supplies.database.export.update;

import com.vanatta.helene.supplies.database.util.HttpPostSender;
import lombok.AllArgsConstructor;

/** Sends updates to 'make' that a new item was created */
@AllArgsConstructor
public class SendNewItemUpdate {
  private final String webhookUrl;
  private final boolean enabled;

  public void sendNewItem(String itemName) {
    if (enabled) {
      new Thread(() -> HttpPostSender.sendJson(webhookUrl, "{\"item-name\":\"" + itemName + "\"}"))
          .start();
    }
  }
}
