package com.vanatta.helene.supplies.database.delivery;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

@Slf4j
public class DeliveryDao {

  public static void upsert(Jdbi jdbi, DeliveryController.DeliveryUpdate deliveryUpdate) {
    assert !deliveryUpdate.getPickupSiteWssId().isEmpty();
    assert !deliveryUpdate.getDropOffSiteWssId().isEmpty();

    String upsert =
        """
        insert into delivery(
          from_site_id, to_site_id, delivery_status, target_delivery_date,
          dispatcher_name, dispatcher_number, driver_name, driver_number,
          driver_license_plates, airtable_id, dispatcher_notes, public_url_key)
        values(
          (select id from site where wss_id = :fromSiteWssId),
          (select id from site where wss_id = :toSiteWssId),
          :deliveryStatus,
          to_date(:targetDeliveryDate, 'YYYY-MM-DD'),
          :dispatcherName,
          :dispatcherNumber,
          :driverName,
          :driverNumber,
          :driverLicensePlateNumbers,
          :airtableId,
          :dispatcherNotes,
          :publicUrlKey
        ) on conflict(airtable_id) do update set
          from_site_id = (select id from site where wss_id = :fromSiteWssId),
          to_site_id = (select id from site where wss_id = :toSiteWssId),
          delivery_status = :deliveryStatus,
          target_delivery_date = to_date(:targetDeliveryDate, 'YYYY-MM-DD'), -- 2024-12-13 SELECT TO_DATE('20170103','YYYYMMDD');
          dispatcher_name = :dispatcherName,
          dispatcher_number = :dispatcherNumber,
          driver_name = :driverName,
          driver_number = :driverNumber,
          driver_license_plates = :driverLicensePlateNumbers,
          dispatcher_notes = :dispatcherNotes
        """;
    jdbi.withHandle(
        handle ->
            handle
                .createUpdate(upsert)
                .bind("fromSiteWssId", deliveryUpdate.getPickupSiteWssId().getFirst())
                .bind("toSiteWssId", deliveryUpdate.getDropOffSiteWssId().getFirst())
                .bind("deliveryStatus", deliveryUpdate.getDeliveryStatus())
                .bind("targetDeliveryDate", deliveryUpdate.getTargetDeliveryDate())
                .bind(
                    "dispatcherName",
                    deliveryUpdate.getDispatcherName().isEmpty()
                        ? null
                        : deliveryUpdate.getDispatcherName().getFirst())
                .bind(
                    "dispatcherNumber",
                    deliveryUpdate.getDispatcherNumber().isEmpty()
                        ? null
                        : deliveryUpdate.getDispatcherNumber().getFirst())
                .bind(
                    "driverName",
                    deliveryUpdate.getDriverName().isEmpty()
                        ? null
                        : deliveryUpdate.getDriverName().getFirst())
                .bind(
                    "driverNumber",
                    deliveryUpdate.getDriverNumber().isEmpty()
                        ? null
                        : deliveryUpdate.getDriverNumber().getFirst())
                .bind(
                    "driverLicensePlateNumbers",
                    deliveryUpdate.getLicensePlateNumbers().isEmpty()
                        ? null
                        : deliveryUpdate.getLicensePlateNumbers().getFirst())
                .bind("airtableId", deliveryUpdate.getDeliveryId())
                .bind("dispatcherNotes", deliveryUpdate.getDispatcherNotes())
                .bind("publicUrlKey", deliveryUpdate.getPublicUrlKey())
                .execute());

    String deletePreviousItems =
        """
      delete from delivery_item where delivery_id =
        (select id from delivery where airtable_id = :deliveryId)
    """;

    jdbi.withHandle(
        handle ->
            handle
                .createUpdate(deletePreviousItems)
                .bind("deliveryId", deliveryUpdate.getDeliveryId())
                .execute());

    // insert all the latest items
    String insert =
        """
    insert into delivery_item(delivery_id, item_id)
    values(
      (select id from delivery where airtable_id = :airtableId),
      (select id from item where wss_id = :itemWssId)
    )
    """;
    List<Long> itemIds = deliveryUpdate.getItemListWssIds();
    for (long itemWssId : itemIds) {
      jdbi.withHandle(
          handle ->
              handle
                  .createUpdate(insert)
                  .bind("airtableId", deliveryUpdate.getDeliveryId())
                  .bind("itemWssId", itemWssId)
                  .execute());
    }
  }

  // get
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DeliveryData {
    long deliveryId;
    String publicUrlKey;
    String deliveryStatus;
    String dispatcherName;
    String dispatcherNumber;
    String dispatcherNotes;
    String driverName;
    String driverNumber;
    String licensePlateNumbers;
    String targetDeliveryDate;

    String fromSiteName;
    long fromSiteId;
    private String fromAddress;
    private String fromCity;
    private String fromState;
    private String fromContactName;
    private String fromContactPhone;
    private String fromHours;

    String toSiteName;
    long toSiteId;
    private String toAddress;
    private String toCity;
    private String toState;
    private String toContactName;
    private String toContactPhone;
    private String toHours;
  }

  public static Delivery fetchDeliveryByPublicKey(Jdbi jdbi, String publicUrlKey) {
    String whereClause = "d.public_url_key = :id";
    var results = fetchDeliveries(jdbi, whereClause, publicUrlKey);
    if (results.isEmpty()) {
      log.warn("Failed to fetch delivery by id (record not found): {}", publicUrlKey);
      throw new IllegalArgumentException("Invalid delivery ID: " + publicUrlKey);
    } else {
      return results.getFirst();
    }
  }

  public static List<Delivery> fetchDeliveriesBySiteId(Jdbi jdbi, Long siteId) {
    String whereClause =
        """
    d.from_site_id = :id
    or d.to_site_id = :id
    """;
    return fetchDeliveries(jdbi, whereClause, siteId);
  }

  private static List<Delivery> fetchDeliveries(Jdbi jdbi, String whereClause, Object idValue) {

    String select =
        String.format(
            """
    select
      d.airtable_id deliveryId,
      d.public_url_key publicUrlKey,
      d.delivery_status deliveryStatus,
      d.target_delivery_date targetDeliveryDate,
      d.dispatcher_name dispatcherName,
      d.dispatcher_number dispatcherNumber,
      d.dispatcher_notes dispatcherNotes,
      d.driver_name driverName,
      d.driver_number driverNumber,
      d.driver_license_plates licensePlateNumbers,

      fromSite.name fromSiteName,
      fromSite.id fromSiteId,
      fromSite.address fromAddress,
      fromSite.city fromCity,
      fromCounty.state fromState,
      fromSite.contact_name fromContactName,
      fromSite.contact_number fromContactPhone,
      fromSite.hours fromHours,

      toSite.name toSiteName,
      toSite.id toSiteId,
      toSite.address toAddress,
      toSite.city toCity,
      toCounty.state toState,
      toSite.contact_name toContactName,
      toSite.contact_number toContactPhone,
      toSite.hours toHours
    from delivery d
    join site fromSite on fromSite.id = d.from_site_id
    join county fromCounty on fromCounty.id = fromSite.county_id
    join site toSite on toSite.id = d.to_site_id
    join county toCounty on toCounty.id = toSite.county_id
    where (%s)
    order by d.target_delivery_date desc
    """,
            whereClause);
    List<Delivery> deliveries =
        jdbi
            .withHandle(
                handle ->
                    handle
                        .createQuery(select)
                        .bind("id", idValue)
                        .mapToBean(DeliveryData.class)
                        .list())
            .stream()
            .map(Delivery::new)
            .toList();

    String selectDeliveryItems =
        """
      select
        i.name
      from delivery_item di
      join item i on i.id = di.item_id
      where di.delivery_id = (select id from delivery where airtable_id = :deliveryId)
      """;

    for (Delivery delivery : deliveries) {
      List<String> items =
          jdbi.withHandle(
              handle ->
                  handle
                      .createQuery(selectDeliveryItems)
                      .bind("deliveryId", delivery.getDeliveryNumber())
                      .mapTo(String.class)
                      .list());
      delivery.getItemList().addAll(items);
    }

    return deliveries;
  }

  public static void deleteDelivery(Jdbi jdbi, long deliveryId) {
    Long databaseId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("select id from delivery where airtable_id = :deliveryId")
                    .bind("deliveryId", deliveryId)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(null));

    if (databaseId == null) {
      return;
    }

    jdbi.withHandle(
        handle ->
            handle
                .createUpdate("delete from delivery_item where delivery_id = :databaseId")
                .bind("databaseId", databaseId)
                .execute());
    jdbi.withHandle(
        handle ->
            handle
                .createUpdate("delete from delivery where id = :databaseId")
                .bind("databaseId", databaseId)
                .execute());
  }
}
