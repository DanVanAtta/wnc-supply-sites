package com.vanatta.helene.supplies.database.incoming.webhook.need.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import com.vanatta.helene.supplies.database.data.SiteType;
import com.vanatta.helene.supplies.database.export.update.SendSiteUpdate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SiteDataImportControllerTest {

  @BeforeAll
  static void setup() {
    TestConfiguration.setupDatabase();
  }

  private SiteDataImportController.SiteUpdate sampleData =
      SiteDataImportController.SiteUpdate.builder()
          .wssId(null) // wssId being null will make us insert data.
          .airtableId((long) (Math.random() * 100_000_000))
          .siteName("SiteName " + UUID.randomUUID())
          .donationStatus(
              SiteDataImportController.DonationStatus.ACCEPTING_DONATIONS.getTextValue())
          .streetAddress("Fake street")
          .city("the city")
          .county("Ashe")
          .state("NC")
          .siteType(List.of("HUB"))
          .facebook("fb url")
          .phone("555-555-5555")
          .hours("these are my hours")
          .email("email yo")
          .website("das good website")
          .pointOfContact("Jane and John")
          .publicVisibility(true)
          .build();

  // test data that is already inserted, can be used to validate the update scenarios.
  private final SiteDataImportController.SiteUpdate updateSampleData =
      sampleData.toBuilder()
          // setting wssId is important, this flags the incoming data as an update
          .wssId((long) (Math.random() * 100_000_000))
          .build();

  private static void insertData(SiteDataImportController.SiteUpdate input) {
    String insertSiteToUpdate =
        """
        insert into site(name, address, city, county_id, site_type_id, airtable_id, wss_id)
        values(
          :siteName,
          :address,
          :city,
          (select id from county where name = :countyName),
          (select id from site_type where name = :siteTypeName),
          :airtableId,
          :wssId
        )
        """;
    TestConfiguration.jdbiTest.withHandle(
        handle ->
            handle
                .createUpdate(insertSiteToUpdate)
                .bind("siteName", input.getSiteName())
                .bind("address", input.getStreetAddress())
                .bind("city", input.getCity())
                .bind("countyName", input.getCounty())
                .bind("siteTypeName", SiteType.DISTRIBUTION_CENTER.getText())
                .bind("airtableId", input.getAirtableId())
                .bind("wssId", input.getWssId())
                .execute());
  }

  private static Map<String, Object> querySite(long airTableId) {
    String selectSiteQuery =
        """
        select s.*, c.name county, c.state
        from site s
        join county c on c.id = s.county_id
        where s.airtable_id = :airtableId
        """;
    return TestConfiguration.jdbiTest.withHandle(
        handle ->
            handle.createQuery(selectSiteQuery).bind("airtableId", airTableId).mapToMap().one());
  }

  private static void validateDataMatches(
      Map<String, Object> results, SiteDataImportController.SiteUpdate input) {
    assertThat(results.get("name")).isEqualTo(input.getSiteName());
    assertThat(results.get("address")).isEqualTo(input.getStreetAddress());
    assertThat(results.get("city")).isEqualTo(input.getCity());
    assertThat(results.get("county")).isEqualTo(input.getCounty());
    assertThat(results.get("state")).isEqualTo(input.getState());
    assertThat(results.get("contact_number")).isEqualTo(input.getPhone());
    assertThat(results.get("website")).isEqualTo(input.getWebsite());
    assertThat((int) results.get("airtable_id")).isEqualTo((int) input.getAirtableId().longValue());
    assertThat(results.get("hours")).isEqualTo(input.getHours());
    assertThat(results.get("contact_name")).isEqualTo(input.getPointOfContact());
    assertThat(results.get("contact_email")).isEqualTo(input.getEmail());
    assertThat(results.get("facebook")).isEqualTo(input.getFacebook());
  }

  private final SiteDataImportController siteDataImportController =
      new SiteDataImportController(
          TestConfiguration.jdbiTest, new SendSiteUpdate(TestConfiguration.jdbiTest, "", false));

  @Nested
  class InsertScenarios {
    /** Insert a happy case amount, where incoming data is the full payload with every field set. */
    @Test
    void insert() {
      var input =
          sampleData.toBuilder()
              .siteName("SiteName " + UUID.randomUUID())
              .airtableId((long) (Math.random() * 100_000_000))
              .build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      // fetch results from DB and validate that everything is updated
      Map<String, Object> results = querySite(input.getAirtableId());
      validateDataMatches(results, input);
    }

    /** Try to insert minimal data and validate we have a lot of null data. */
    @Test
    void insertMinimalData() {
      var input =
          sampleData.toBuilder()
              .siteName("SiteName " + UUID.randomUUID())
              .airtableId((long) (Math.random() * 100_000_000))
              .pointOfContact(null)
              .website(null)
              .hours(null)
              .phone(null)
              .email(null)
              .facebook(null)
              .build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      // fetch results from DB and validate that everything is updated
      Map<String, Object> results = querySite(input.getAirtableId());
      validateDataMatches(results, input);
    }

    @Test
    void missingCountyStatePairsAreCreated() {
      var input =
          sampleData.toBuilder()
              .siteName("SiteName " + UUID.randomUUID())
              .airtableId((long) (Math.random() * 100_000_000))
              .county("new county")
              .state("VA")
              .build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      // fetch results from DB and validate that everything is updated
      Map<String, Object> results = querySite(input.getAirtableId());
      assertThat(results.get("state")).isEqualTo("VA");
      assertThat(results.get("county")).isEqualTo("new county");
    }
  }

  @Nested
  class UpdateScenarios {

    /** Insert a site that we will later use for update testing. */
    @BeforeEach
    void setupSiteForUpdate() {
      insertData(updateSampleData);
    }

    @Test
    void updateMostEverything() {
      var input =
          updateSampleData.toBuilder()
              .siteName("SiteName " + UUID.randomUUID())
              .streetAddress("new street")
              .county("Buncombe")
              .state("NC")
              .siteType(List.of("POD"))
              .facebook("new FB")
              .phone("555-555-1111")
              .hours("updated hours!")
              .email("new email yeah")
              .website("das updated website")
              .pointOfContact("Sam and Sandy")
              .publicVisibility(false)
              .build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      Map<String, Object> results = querySite(input.getAirtableId());
      validateDataMatches(results, input);
    }

    @Test
    void deleteMostData() {
      var input =
          updateSampleData.toBuilder()
              .siteName("SiteName " + UUID.randomUUID())
              .facebook(null)
              .phone(null)
              .hours(null)
              .email(null)
              .website(null)
              .pointOfContact(null)
              .build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      Map<String, Object> results = querySite(input.getAirtableId());
      validateDataMatches(results, input);
    }
  }

  /**
   * For all of the below scenarios, fields can have different values and we need to be sure we
   * handle those values correctly on both insert and update cases.
   */
  @Nested
  class MultiValuedFieldScenarios {

    /** Insert a site that we will later use for update testing. */
    @BeforeEach
    void setupSiteForUpdate() {
      insertData(updateSampleData);
      sampleData =
          sampleData.toBuilder()
              .siteName("SiteName " + UUID.randomUUID())
              .airtableId((long) (Math.random() * 100_000_000))
              .build();
    }

    @Builder
    @Value
    static class SiteTypeTestScenario {
      @NonNull List<String> inputSiteTypes;
      @NonNull String expectedSiteType;
      @NonNull Boolean expectedDistributingSupplies;
    }

    @ParameterizedTest
    @MethodSource
    void siteType(SiteTypeTestScenario scenario) {

      // INSERT CASE
      var input = sampleData.toBuilder().siteType(scenario.inputSiteTypes).build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      String siteType = queryForSiteType(input.getAirtableId());
      assertThat(siteType).isEqualTo(scenario.expectedSiteType);
      boolean distributingDonations = queryForDistributingDonations(input.getAirtableId());
      assertThat(distributingDonations).isEqualTo(scenario.expectedDistributingSupplies);

      // UPDATE CASE
      input = updateSampleData.toBuilder().siteType(scenario.inputSiteTypes).build();
      response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      siteType = queryForSiteType(input.getAirtableId());
      assertThat(siteType).isEqualTo(scenario.expectedSiteType);
      distributingDonations = queryForDistributingDonations(input.getAirtableId());
      assertThat(distributingDonations).isEqualTo(scenario.expectedDistributingSupplies);
    }

    private static String queryForSiteType(long airtableId) {
      String query =
          """
          select st.name
          from site s
          join site_type st on st.id = s.site_type_id
          where airtable_id = :airtableId
          """;
      return TestConfiguration.jdbiTest.withHandle(
          handle ->
              handle.createQuery(query).bind("airtableId", airtableId).mapTo(String.class).one());
    }

    private static boolean queryForDistributingDonations(long airtableId) {
      String query =
          """
          select s.distributing_supplies
          from site s
          where airtable_id = :airtableId
          """;
      return TestConfiguration.jdbiTest.withHandle(
          handle ->
              handle.createQuery(query).bind("airtableId", airtableId).mapTo(Boolean.class).one());
    }

    /**
     * Variety of cases of incoming site types. Generally anything that has HUB in it, is just a HUB
     */
    static List<SiteTypeTestScenario> siteType() {
      return List.of(
          SiteTypeTestScenario.builder()
              .inputSiteTypes(List.of("HUB"))
              .expectedSiteType(SiteType.SUPPLY_HUB.getText())
              .expectedDistributingSupplies(false)
              .build(),
          SiteTypeTestScenario.builder()
              .inputSiteTypes(List.of("POD"))
              .expectedSiteType(SiteType.DISTRIBUTION_CENTER.getText())
              .expectedDistributingSupplies(true)
              .build(),
          SiteTypeTestScenario.builder()
              .inputSiteTypes(List.of("POD", "POC"))
              .expectedSiteType(SiteType.DISTRIBUTION_CENTER.getText())
              .expectedDistributingSupplies(true)
              .build(),
          SiteTypeTestScenario.builder()
              .inputSiteTypes(List.of("HUB", "POD"))
              .expectedSiteType(SiteType.SUPPLY_HUB.getText())
              .expectedDistributingSupplies(true)
              .build(),
          SiteTypeTestScenario.builder()
              .inputSiteTypes(List.of("HUB", "POC"))
              .expectedSiteType(SiteType.SUPPLY_HUB.getText())
              .expectedDistributingSupplies(false)
              .build(),
          SiteTypeTestScenario.builder()
              .inputSiteTypes(List.of("HUB", "POD", "POC"))
              .expectedSiteType(SiteType.SUPPLY_HUB.getText())
              .expectedDistributingSupplies(true)
              .build());
    }

    /**
     * Invalid site types should generally be ignored and default to not accepting donations &
     * distribution site type
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "NGO", "DNE"})
    void siteTypeIllegalValues(String illegalSiteType) {
      var input = sampleData.toBuilder().siteType(List.of(illegalSiteType)).build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      String siteType = queryForSiteType(input.getAirtableId());
      assertThat(siteType).isEqualTo(SiteType.DISTRIBUTION_CENTER.getText());
      boolean distributingDonations = queryForDistributingDonations(input.getAirtableId());
      assertThat(distributingDonations).isEqualTo(false);
    }

    @Test
    void emptySiteType() {
      var input = sampleData.toBuilder().siteType(List.of()).build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      String siteType = queryForSiteType(input.getAirtableId());
      assertThat(siteType).isEqualTo(SiteType.DISTRIBUTION_CENTER.getText());
      boolean distributingDonations = queryForDistributingDonations(input.getAirtableId());
      assertThat(distributingDonations).isEqualTo(false);
    }

    /**
     * Expected data pairs are incoming donation status and expected value of accepting donations.
     */
    @ParameterizedTest
    @CsvSource({
      "Accepting Donations,true",
      "Accepting Requested Donations Only,true",
      "Not Accepting Donations,false",
      "Closed,false"
    })
    void donationStatus(String inputDonationStatus, String expectedIsAcceptingDonations) {
      // INSERT CASE
      var input = sampleData.toBuilder().donationStatus(inputDonationStatus).build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      var donationStatus = queryForDonationStatus(input.getAirtableId());
      assertThat(donationStatus).isEqualTo(Boolean.valueOf(expectedIsAcceptingDonations));

      // UPDATE CASE
      input = updateSampleData.toBuilder().donationStatus(inputDonationStatus).build();

      response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      donationStatus = queryForDonationStatus(input.getAirtableId());
      assertThat(donationStatus).isEqualTo(Boolean.valueOf(expectedIsAcceptingDonations));
    }

    static boolean queryForDonationStatus(long airtableId) {
      String query =
          """
          select accepting_donations from site where airtable_id = :airtableId
          """;
      return TestConfiguration.jdbiTest.withHandle(
          handle ->
              handle.createQuery(query).bind("airtableId", airtableId).mapTo(Boolean.class).one());
    }

    /** Expected data pairs are incoming donation status and expected value of active. */
    @ParameterizedTest
    @CsvSource({
      "Accepting Donations,true",
      "Accepting Requested Donations Only,true",
      "Not Accepting Donations,true",
      "Closed,false"
    })
    void active(String inputDonationStatus, String expectedActive) {
      // INSERT CASE
      var input = sampleData.toBuilder().donationStatus(inputDonationStatus).build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      var activeStatus = queryForActiveStatus(input.getAirtableId());
      assertThat(activeStatus).isEqualTo(Boolean.valueOf(expectedActive));

      // UPDATE CASE
      input = updateSampleData.toBuilder().donationStatus(inputDonationStatus).build();

      response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      activeStatus = queryForActiveStatus(input.getAirtableId());
      assertThat(activeStatus).isEqualTo(Boolean.valueOf(expectedActive));
    }

    static boolean queryForActiveStatus(long airtableId) {
      String query =
          """
          select active from site where airtable_id = :airtableId
          """;
      return TestConfiguration.jdbiTest.withHandle(
          handle ->
              handle.createQuery(query).bind("airtableId", airtableId).mapTo(Boolean.class).one());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void publicVisibility(boolean isPubliclyVisible) {
      // INSERT CASE
      var input = sampleData.toBuilder().publicVisibility(isPubliclyVisible).build();

      var response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      var publiclyVisible = queryForPubliclyVisible(input.getAirtableId());
      assertThat(publiclyVisible).isEqualTo(isPubliclyVisible);

      // UPDATE CASE
      input = updateSampleData.toBuilder().publicVisibility(isPubliclyVisible).build();

      response = siteDataImportController.updateSiteData(input);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      publiclyVisible = queryForPubliclyVisible(input.getAirtableId());
      assertThat(publiclyVisible).isEqualTo(isPubliclyVisible);
    }

    static boolean queryForPubliclyVisible(long airtableId) {
      String query =
          """
          select publicly_visible from site where airtable_id = :airtableId
          """;
      return TestConfiguration.jdbiTest.withHandle(
          handle ->
              handle.createQuery(query).bind("airtableId", airtableId).mapTo(Boolean.class).one());
    }
  }
}
