package com.vanatta.helene.supplies.database.delivery;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data object associated with a delivery, tracks the confirmations received for a deliver. Does NOT
 * track dispatcher confirmation. Confirmations are created when the dispatcher gives a very first
 * confirmation.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryConfirmation {

  @Nonnull private String confirmRole;

  /** Null means no decision has been made, false -> cancel, true -> confirmed */
  @Nullable private Boolean confirmed;

  @Nonnull private String code;

  public enum ConfirmRole {
    DRIVER,
    PICKUP_SITE,
    DROPOFF_SITE;
  }
}
