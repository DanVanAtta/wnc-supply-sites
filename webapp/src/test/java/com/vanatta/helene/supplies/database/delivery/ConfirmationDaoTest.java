package com.vanatta.helene.supplies.database.delivery;

import static com.vanatta.helene.supplies.database.TestConfiguration.jdbiTest;
import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ConfirmationDaoTest {

  @BeforeEach
  void setup() {
    TestConfiguration.setupDatabase();
  }

  @Test
  void noConfirmationsInSystem() {
    Delivery delivery = DeliveryHelper.withNewDelivery();

    assertThat(delivery.getDriverConfirmationCode()).isNull();
    assertThat(delivery.getPickupConfirmationCode()).isNull();
    assertThat(delivery.getDropOffConfirmationCode()).isNull();
    assertThat(delivery.getConfirmations()).isEmpty();
  }

  /**
   * Once a dispatcher confirms, we should create requests for confirmation. Validate that we create
   * these requests with the secret codes.
   */
  @Test
  void codesAreGeneratedAfterDispatcherConfirm_and_confirmationsAreDefault() {
    Delivery delivery = DeliveryHelper.withDispatcherConfirmedDelivery();

    assertThat(delivery.getDriverConfirmationCode()).isNotNull();
    assertThat(delivery.getPickupConfirmationCode()).isNotNull();
    assertThat(delivery.getDropOffConfirmationCode()).isNotNull();

    assertThat(delivery.getConfirmations()).isNotEmpty();
    assertThat(delivery.getConfirmations())
        .hasSize(DeliveryConfirmation.ConfirmRole.values().length);

    for (DeliveryConfirmation confirmation : delivery.getConfirmations()) {
      assertThat(confirmation.getConfirmed()).isNull();
      assertThat(confirmation.getCode()).isNotNull();
      assertThat(confirmation.getConfirmRole()).isNotNull();
    }
  }

  /** Dispatcher confirm, then after driver/sites confirms, validate that it shows as confirmed. */
  @Test
  void confirmations() {
    var delivery = DeliveryHelper.withDispatcherConfirmedDelivery();

    ConfirmationDao.confirmDelivery(
        jdbiTest, delivery.getPublicKey(), DeliveryConfirmation.ConfirmRole.DRIVER);
    ConfirmationDao.confirmDelivery(
        jdbiTest, delivery.getPublicKey(), DeliveryConfirmation.ConfirmRole.DROPOFF_SITE);
    ConfirmationDao.confirmDelivery(
        jdbiTest, delivery.getPublicKey(), DeliveryConfirmation.ConfirmRole.PICKUP_SITE);

    var confirmations =
        DeliveryDao.fetchDeliveryByPublicKey(jdbiTest, delivery.getPublicKey())
            .orElseThrow()
            .getConfirmations();
    assertThat(confirmations).isNotEmpty();
    confirmations.forEach(confirmation -> assertThat(confirmation.getConfirmed()).isTrue());
  }

  /** Dispatcher confirm, then after driver/sites confirms, validate that it shows as confirmed. */
  @Test
  void cancellations() {
    var delivery = DeliveryHelper.withDispatcherConfirmedDelivery();

    ConfirmationDao.cancelDelivery(
        jdbiTest, delivery.getPublicKey(), DeliveryConfirmation.ConfirmRole.DRIVER);
    ConfirmationDao.cancelDelivery(
        jdbiTest, delivery.getPublicKey(), DeliveryConfirmation.ConfirmRole.DROPOFF_SITE);
    ConfirmationDao.cancelDelivery(
        jdbiTest, delivery.getPublicKey(), DeliveryConfirmation.ConfirmRole.PICKUP_SITE);

    var confirmations =
        DeliveryDao.fetchDeliveryByPublicKey(jdbiTest, delivery.getPublicKey())
            .orElseThrow()
            .getConfirmations();

    assertThat(confirmations).isNotEmpty();
    confirmations.forEach(confirmation -> assertThat(confirmation.getConfirmed()).isFalse());
  }

  @ParameterizedTest
  @EnumSource(DriverStatus.class)
  void updateDriverStatus(DriverStatus driverStatus) {
    var delivery = DeliveryHelper.withNewDelivery();

    ConfirmationDao.updateDriverStatus(jdbiTest, delivery.getPublicKey(), driverStatus);
    assertThat(
            DeliveryDao.fetchDeliveryByPublicKey(jdbiTest, delivery.getPublicKey())
                .orElseThrow()
                .getDriverStatus())
        .isEqualTo(driverStatus.name());
  }
}
