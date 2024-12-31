package com.vanatta.helene.supplies.database.manage.contact;

import static com.vanatta.helene.supplies.database.TestConfiguration.jdbiTest;
import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContactDaoTest {

  @BeforeEach
  void setUp() {
    jdbiTest.withHandle(
        handle ->
            handle
                .createScript(
                    """
      delete from additional_site_manager;
      """)
                .execute());
  }

  final ContactDao.SiteManager manager =
      ContactDao.SiteManager.builder()
          .name("my new name") //
          .phone("12345")
          .build();

  @Test
  void insertAndRead() {
    long siteId = TestConfiguration.getSiteId("site1");

    ContactDao.addAdditionalSiteManager(jdbiTest, siteId, manager.getName(), manager.getPhone());

    List<ContactDao.SiteManager> managers = ContactDao.getManagers(jdbiTest, siteId);

    assertThat(managers).hasSize(1);
    assertThat(managers.getFirst().getName()).isEqualTo(manager.getName());
    assertThat(managers.getFirst().getPhone()).isEqualTo(manager.getPhone());
  }

  @Test
  void updateManager() {
    long siteId = TestConfiguration.getSiteId("site1");

    ContactDao.addAdditionalSiteManager(jdbiTest, siteId, manager.getName(), manager.getPhone());

    long newId = ContactDao.getManagers(jdbiTest, siteId).getFirst().getId();

    var update = manager.toBuilder().id(newId).name("update name").phone("0000").build();

    ContactDao.updateAdditionalSiteManager(jdbiTest, siteId, update);

    List<ContactDao.SiteManager> managers = ContactDao.getManagers(jdbiTest, siteId);

    assertThat(managers).hasSize(1);
    assertThat(managers.getFirst()).isEqualTo(update);
  }

  @Test
  void removeManager() {
    long siteId = TestConfiguration.getSiteId("site1");

    long managerId =
        ContactDao.addAdditionalSiteManager(
            jdbiTest, siteId, manager.getName(), manager.getPhone());
    ContactDao.removeAdditionalSiteManager(jdbiTest, siteId, managerId);

    List<ContactDao.SiteManager> managers = ContactDao.getManagers(jdbiTest, siteId);

    assertThat(managers).isEmpty();
  }
}
