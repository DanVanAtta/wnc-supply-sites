package com.vanatta.helene.supplies.database.manage.add.site;

import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import com.vanatta.helene.supplies.database.data.SiteType;
import com.vanatta.helene.supplies.database.export.update.SendSiteUpdate;
import com.vanatta.helene.supplies.database.supplies.site.details.SiteDetailDao;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AddSiteControllerTest {

  AddSiteController addSiteController =
      new AddSiteController(TestConfiguration.jdbiTest, SendSiteUpdate.newDisabled());

  @Test
  void addSite() {
    String siteName = UUID.randomUUID().toString();

    Map<String, String> newSiteParams = new HashMap<>();
    newSiteParams.put("siteName", siteName);
    newSiteParams.put("streetAddress", "address");
    newSiteParams.put("city", "city");
    newSiteParams.put("state", "NC");
    newSiteParams.put("county", "Watauga");
    newSiteParams.put("website", "website");
    newSiteParams.put("facebook", "facebook");
    newSiteParams.put("siteType", SiteType.SUPPLY_HUB.getText());
    newSiteParams.put("siteHours", "siteHours");

    newSiteParams.put("hasForklift", "true");
    newSiteParams.put("hasLoadingDock", "true");
    newSiteParams.put("hasIndoorStorage", "true");
    newSiteParams.put("maxSupplyLoad", "Car");
    newSiteParams.put("receivingNotes", "notes");

    newSiteParams.put("contactName", "contactName");
    newSiteParams.put("contactNumber", "contactNumber");
    newSiteParams.put("contactEmail", "contactEmail");
    newSiteParams.put("additionalContacts", "additionalContacts");

    ResponseEntity<String> result = addSiteController.postNewSite(newSiteParams);

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody()).contains("editSiteInventoryUrl");

    SiteDetailDao.SiteDetailData data =
        SiteDetailDao.lookupSiteById(
            TestConfiguration.jdbiTest, TestConfiguration.getSiteId(siteName));

    assertThat(data.getSiteName()).isEqualTo(siteName);
    assertThat(data.getAddress()).isEqualTo("address");
    assertThat(data.getCity()).isEqualTo("city");
    assertThat(data.getState()).isEqualTo("NC");
    assertThat(data.getCounty()).isEqualTo("Watauga");
    assertThat(data.getWebsite()).isEqualTo("website");
    assertThat(data.getFacebook()).isEqualTo("facebook");
    assertThat(data.getSiteType()).isEqualTo(SiteType.SUPPLY_HUB.getText());
    assertThat(data.getHours()).isEqualTo("siteHours");

    assertThat(data.isHasForklift()).isTrue();
    assertThat(data.isHasLoadingDock()).isTrue();
    assertThat(data.isHasIndoorStorage()).isTrue();
    assertThat(data.getMaxSupply()).isEqualTo("Car");
    assertThat(data.getReceivingNotes()).isEqualTo("notes");

    assertThat(data.getContactName()).isEqualTo("contactName");
    assertThat(data.getContactNumber()).isEqualTo("contactNumber");
    assertThat(data.getContactEmail()).isEqualTo("contactEmail");
    assertThat(data.getAdditionalContacts()).isEqualTo("additionalContacts");
  }
}
