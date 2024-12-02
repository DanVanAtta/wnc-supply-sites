package com.vanatta.helene.supplies.database.manage;

import com.vanatta.helene.supplies.database.data.ItemStatus;
import com.vanatta.helene.supplies.database.manage.ManageSiteController.SiteSelection;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

@Slf4j
public class ManageSiteDao {

  static List<SiteSelection> fetchSiteList(Jdbi jdbi) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("select id, name from site order by lower(name)")
                .mapToBean(SiteSelection.class)
                .list());
  }

  @AllArgsConstructor
  @Getter
  public enum SiteField {
    SITE_NAME("name", "Site Name", true),
    CONTACT_NUMBER("contact_number", "Contact Number", false),
    WEBSITE("website", "Website", false),
    STREET_ADDRESS("address", "Street Address", true),
    CITY("city", "City", true),
    COUNTY("county", "County", true),
    STATE("state", "State", true),
    ;

    private final String columnName;
    private final String frontEndName;
    private final boolean required;

    static SiteField lookupField(String name) {
      return Arrays.stream(SiteField.values())
          .filter(f -> f.frontEndName.equals(name))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException("Invalid field name: " + name));
    }
  }

  static class RequiredFieldException extends IllegalArgumentException {
    RequiredFieldException(String fieldName) {
      super("Required field " + fieldName + " cannot be deleted");
    }
  }

  public static void updateSiteField(Jdbi jdbi, long siteId, SiteField field, String newValue) {
    log.info("Updating site: {}, field: {}, value: {}", siteId, field, newValue);

    if (field.isRequired() && (newValue == null || newValue.isEmpty())) {
      throw new RequiredFieldException(field.frontEndName);
    }

    if (field == SiteField.COUNTY) {
      updateCounty(jdbi, siteId, Optional.ofNullable(newValue).orElse(""));
    } else {
      updateSiteColumn(jdbi, siteId, field.getColumnName(), newValue);
    }
  }

  /** Updating a county potentially requires us to create the county first, before updating it. */
  private static void updateCounty(Jdbi jdbi, long siteId, String newValue) {
    String selectCounty =
        """
          select id from county where name = :name
        """;
    final Long countyId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(selectCounty)
                    .bind("name", newValue)
                    .mapTo(Long.class)
                    .findOne()
                    .orElse(null));
    if (countyId == null) {
      log.warn("Failed to update county for site id: {}, county value: {}", siteId, newValue);
      throw new IllegalArgumentException("Invalid county: " + newValue);
    }

    String updateCounty =
        """
        update site set county_id = :countyId where id = :id
        """;
    jdbi.withHandle(
        handle ->
            handle
                .createUpdate(updateCounty)
                .bind("countyId", countyId)
                .bind("id", siteId)
                .execute());
  }

  private static void updateSiteColumn(Jdbi jdbi, long siteId, String column, String newValue) {
    jdbi.withHandle(
        handle ->
            handle
                .createUpdate("update site set " + column + " = :newValue where id = :siteId")
                .bind("newValue", newValue)
                .bind("siteId", siteId)
                .execute());
  }

  @Nullable
  public static String fetchSiteName(Jdbi jdbi, long siteId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("select name from site where id = :siteId")
                .bind("siteId", siteId)
                .mapTo(String.class)
                .one());
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SiteStatus {
    boolean active;
    boolean acceptingDonations;
    String siteType;

    public SiteType getSiteTypeEnum() {
      return Arrays.stream(SiteType.values())
          .filter(s -> s.siteTypeName.equals(siteType))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException("Unknown site type: " + siteType));
    }
  }

  public static SiteStatus fetchSiteStatus(Jdbi jdbi, long siteId) {
    String query =
        """
        select s.active, s.accepting_donations, st.name siteType
        from site s
        join site_type st on st.id = s.site_type_id
        where s.id = :siteId
        """;

    SiteStatus siteStatus =
        jdbi.withHandle(
            handle ->
                handle.createQuery(query).bind("siteId", siteId).mapToBean(SiteStatus.class).one());
    if (siteStatus == null) {
      throw new IllegalArgumentException("Invalid site id: " + siteId);
    } else {
      return siteStatus;
    }
  }

  public static void updateSiteAcceptingDonationsFlag(Jdbi jdbi, long siteId, boolean newValue) {
    int updateCount =
        jdbi.withHandle(
            handle ->
                handle
                    .createUpdate(
                        "update site set accepting_donations = :newValue, last_updated = now() where id = :siteId")
                    .bind("newValue", newValue)
                    .bind("siteId", siteId)
                    .execute());

    if (updateCount == 0) {
      throw new IllegalArgumentException("Invalid site id: " + siteId);
    }
    updateSiteLastUpdatedToNow(jdbi, siteId);
  }

  public static void updateSiteActiveFlag(Jdbi jdbi, long siteId, boolean newValue) {
    int updateCount =
        jdbi.withHandle(
            handle ->
                handle
                    .createUpdate(
                        "update site set active = :newValue, last_updated = now() where id = :siteId")
                    .bind("newValue", newValue)
                    .bind("siteId", siteId)
                    .execute());

    if (updateCount == 0) {
      throw new IllegalArgumentException("Invalid site id: " + siteId);
    }
    updateSiteLastUpdatedToNow(jdbi, siteId);
  }

  /** Fetches all items, items requested/needed for a given site are listed as active. */
  public static List<SiteInventory> fetchSiteInventory(Jdbi jdbi, long siteId) {
    String query =
        """
        with inventory as (
          select
              i.id item_id,
              s.id site_id,
              stat.name status_name
         from site s
         join site_item si on si.site_id = s.id
         join item i on i.id = si.item_id
         join item_status stat on stat.id = si.item_status_id
         where s.id = :siteId
        )
        select
            i.id item_id,
            i.name item_name,
            case when inv.site_id is null then false else true end active,
            inv.status_name item_status
        from item i
        left join inventory inv on inv.item_id = i.id
    """;

    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(query)
                .bind("siteId", siteId) //
                .mapToBean(SiteInventory.class)
                .list());
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SiteInventory {
    long itemId;
    String itemName;
    String itemStatus;
    boolean active;
  }

  public static void updateSiteLastUpdatedToNow(Jdbi jdbi, long siteId) {
    String updateSiteLastUpdated = "update site set last_updated = now() where id = :siteId";
    jdbi.withHandle(
        handle -> handle.createUpdate(updateSiteLastUpdated).bind("siteId", siteId).execute());
  }


  @AllArgsConstructor
  enum SiteType {
    DISTRIBUTION_SITE("Distribution Center"),
    SUPPLY_HUB("Supply Hub"),
    ;
    String siteTypeName;
  }

  static void updateSiteType(Jdbi jdbi, long siteId, SiteType siteType) {
    String update =
        """
        update site set site_type_id = (select id from site_type where name = :siteTypeName)
           where id = :siteId;
        """;
    jdbi.withHandle(
        handle ->
            handle
                .createUpdate(update)
                .bind("siteId", siteId)
                .bind("siteTypeName", siteType.siteTypeName)
                .execute());
  }
}
