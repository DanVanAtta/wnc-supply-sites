package com.vanatta.helene.supplies.database.supplies.site.details;

import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import com.vanatta.helene.supplies.database.supplies.site.details.SiteDetailDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SiteDetailDaoTest {

  @BeforeAll
  static void setUp() {
    TestConfiguration.setupDatabase();
  }

  @Test
  void lookupSite() {
    long idToLookup =
        TestConfiguration.jdbiTest.withHandle(
            handle ->
                handle
                    .createQuery("select id from site where name = 'site1'")
                    .mapTo(Long.class)
                    .one());

    var result = SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, idToLookup);

    assertThat(result.getSiteName()).isEqualTo("site1");
    assertThat(result.getAddress()).isEqualTo("address1");
    assertThat(result.getCity()).isEqualTo("city1");
    assertThat(result.getState()).isEqualTo("NC");
    assertThat(result.getWebsite()).isEqualTo("site1website");
    assertThat(result.getCounty()).isEqualTo("Watauga");
  }

  @Test
  void nonExistentSiteThrows() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> SiteDetailDao.lookupSiteById(TestConfiguration.jdbiTest, -1));
  }
}
