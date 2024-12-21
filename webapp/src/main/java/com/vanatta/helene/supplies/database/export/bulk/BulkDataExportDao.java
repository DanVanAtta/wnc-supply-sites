package com.vanatta.helene.supplies.database.export.bulk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jdbi.v3.core.Jdbi;

class BulkDataExportDao {

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ItemExportDbEntry {
    String name;
    long wssId;
  }

  static List<ItemExportDbEntry> getAllItems(Jdbi jdbi) {
    String query =
        """
          select name, wss_id from item order by lower(name)
        """;
    return jdbi.withHandle(
        handle -> handle.createQuery(query).mapToBean(ItemExportDbEntry.class).list());
  }

  static List<SiteExportJson> fetchAllSites(Jdbi jdbi) {
    String fetchSiteDataQuery =
        """
            select
              s.name siteName,
              s.wss_id wssId,
              case when st.name = 'Distribution Center' then 'POD,POC' else 'POD,POC,HUB' end siteType,
              s.contact_number,
              s.address,
              s.city,
              c.state,
              s.website,
              c.name county,
              case when not active
                then 'Closed'
                else case when s.accepting_donations then 'Accepting Donations' else 'Not Accepting Donations' end
              end donationStatus,
              s.active,
              string_agg(i.name, ',') filter (where its.name in ('Urgently Needed')) urgentlyNeeded,
              string_agg(i.name, ',') filter (where its.name in ('Needed')) needed,
              string_agg(i.name, ',') filter (where its.name in ('Available')) available,
              string_agg(i.name, ',') filter (where its.name in ('Oversupply')) oversupply
            from site s
            join county c on c.id = s.county_id
            join site_type st on st.id = s.site_type_id
            left join site_item si on s.id = si.site_id
            left join item i on i.id = si.item_id
            left join item_status its on its.id = si.item_status_id
            group by s.name, s.wss_id, siteType, s.contact_number, s.address, s.city,
             c.state, s.website, county, donationStatus, s.active
            """;

    return jdbi
        .withHandle(
            handle -> handle.createQuery(fetchSiteDataQuery).mapToBean(SiteDataResult.class).list())
        .stream()
        .map(SiteExportJson::new)
        .toList();
  }

  /** Data that can be sent as JSON to sevice. */
  @Value
  public static class SiteExportJson {
    String siteName;
    long wssId;
    String oldName;
    List<String> siteType;
    String contactNumber;
    String address;
    String city;
    String state;
    String county;
    String website;
    String donationStatus;
    boolean active;
    List<String> urgentlyNeededItems;
    List<String> neededItems;
    List<String> availableItems;
    List<String> oversupplyItems;

    SiteExportJson(SiteDataResult result) {
      this.siteName = result.getSiteName();
      oldName = this.siteName;
      this.wssId = result.getWssId();
      this.siteType = Arrays.asList(result.getSiteType().split(","));
      this.contactNumber = result.getContactNumber();
      this.address = result.getAddress();
      this.city = result.getCity();
      this.state = result.getState();
      this.county = result.getCounty();
      this.website = result.getWebsite();
      this.donationStatus = result.getDonationStatus();
      this.active = result.isActive();

      this.urgentlyNeededItems = extractField(result, SiteDataResult::getUrgentlyNeeded);
      this.neededItems = extractField(result, SiteDataResult::getNeeded);
      this.availableItems = extractField(result, SiteDataResult::getAvailable);
      this.oversupplyItems = extractField(result, SiteDataResult::getOverSupply);
    }

    private static List<String> extractField(
        SiteDataResult result, Function<SiteDataResult, String> mapping) {
      String value = mapping.apply(result);
      return value == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(value.split(",")));
    }
  }

  /** Represents DB data for one site. */
  @Data
  @NoArgsConstructor
  public static class SiteDataResult {
    String siteName;
    long wssId;
    String siteType;
    String contactNumber;
    String address;
    String city;
    String state;
    String county;
    String website;
    String donationStatus;
    boolean active;

    /** Items are encoded as a comma delimited list */
    String urgentlyNeeded;

    String needed;
    String available;
    String overSupply;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class NeedRequestDbResult {
    String needRequestId;
    String site;
    String status;
    String priority;

    /** Comma delimited */
    String suppliesNeeded;

    /** Comma delimited */
    String suppliesUrgentlyNeeded;
  }

  @Value
  public static class NeedRequest {
    String needRequestId;
    String site;
    String status;
    List<String> suppliesNeeded;
    List<String> suppliesUrgentlyNeeded;
  }
}
