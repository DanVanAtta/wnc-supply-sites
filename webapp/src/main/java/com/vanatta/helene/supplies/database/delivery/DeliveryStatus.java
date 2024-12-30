package com.vanatta.helene.supplies.database.delivery;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
enum DeliveryStatus {
  CREATING_DISPATCH("Creating Dispatch"),
  ASSIGNING_DRIVER("Assigning Driver"),
  CONFIRMING("Confirming"),
  CONFIRMED("Confirmed"),
  DELIVERY_IN_PROGRESS("Delivery In Progress"),
  DELIVERY_COMPLETED("Delivery Completed"),
  DELIVERY_CANCELLED("Delivery Cancelled"),
  ;

  final String airtableName;
}
