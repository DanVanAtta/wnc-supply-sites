package com.vanatta.helene.supplies.database.delivery;

import static com.vanatta.helene.supplies.database.TestConfiguration.jdbiTest;
import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendDeliveryUpdateTest {
  String domain = "http://localhost:8080";
  SendDeliveryUpdate sendDeliveryUpdate = new SendDeliveryUpdate(jdbiTest, false, "");

  @BeforeEach
  void setup() {
    TestConfiguration.setupDatabase();
  }

  @Test
  void createPayload() {
    var delivery = DeliveryHelper.withConfirmedDelivery();

    var payload =
        sendDeliveryUpdate.createPayload(
            jdbiTest, delivery.getPublicKey(), DeliveryStatus.DELIVERY_IN_PROGRESS, domain);

    assertThat(payload.getAirtableId()).isEqualTo(delivery.getDeliveryNumber());
    assertThat(payload.getDeliveryStatus())
        .isEqualTo(DeliveryStatus.DELIVERY_IN_PROGRESS.getAirtableName());
    assertThat(payload.getDriverStatus()).isEqualTo(delivery.getDriverStatus());

    assertThat(payload.getPickupConfirmLink())
        .isEqualTo(
            String.format(
                "http://localhost:8080/delivery/%s?code=%s",
                delivery.getPublicKey(),
                delivery
                    .getConfirmation(DeliveryConfirmation.ConfirmRole.PICKUP_SITE)
                    .orElseThrow()
                    .getCode()));

    assertThat(payload.getDropOffConfirmLink())
        .isEqualTo(
            String.format(
                "http://localhost:8080/delivery/%s?code=%s",
                delivery.getPublicKey(),
                delivery
                    .getConfirmation(DeliveryConfirmation.ConfirmRole.DROPOFF_SITE)
                    .orElseThrow()
                    .getCode()));

    assertThat(payload.getDriverConfirmLink())
        .isEqualTo(
            String.format(
                "http://localhost:8080/delivery/%s?code=%s",
                delivery.getPublicKey(),
                delivery
                    .getConfirmation(DeliveryConfirmation.ConfirmRole.DRIVER)
                    .orElseThrow()
                    .getCode()));
  }
}
