package com.vanatta.helene.supplies.database.auth.setup.password.send.access.code;

import static org.assertj.core.api.Assertions.assertThat;

import com.vanatta.helene.supplies.database.TestConfiguration;
import com.vanatta.helene.supplies.database.auth.setup.password.SetupPasswordHelper;
import com.vanatta.helene.supplies.database.twilio.sms.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class SendAccessTokenControllerTest {

  private static final String accessCode = "123456";
  private static final String csrf = "csrf";

  private final SendAccessTokenController controller =
      new SendAccessTokenController(
          SmsSender.newDisabled(TestConfiguration.jdbiTest),
          TestConfiguration.jdbiTest,
          new AccessTokenGenerator() {
            @Override
            public String generate() {
              return accessCode;
            }
          },
          () -> csrf);

  static final String number = "1111111111";
  static final String input = String.format("{\"number\":\"%s\"}", number);

  @BeforeEach
  void setup() {
    SetupPasswordHelper.setup();
  }

  @Nested
  class RequestParsing {

    @Test
    void canParse() {
      SendAccessTokenController.SendAccessCodeRequest result =
          SendAccessTokenController.SendAccessCodeRequest.parse(input);
      assertThat(result.getNumber()).isEqualTo(number);
    }
  }

  @Test
  void sendAccessCode_case_notRegistered() {
    ResponseEntity<SendAccessTokenController.SendAccessCodeResponse> response =
        controller.sendAccessCode(input);
    assertThat(response.getStatusCode().value()).isEqualTo(401);
    assertThat(response.getBody().getError()).isNotNull();
    assertThat(response.getBody().getCsrf()).isNull();
  }

  @Test
  void sendAccessCode_case_registered() {
    assertThat(SetupPasswordHelper.accessTokenExists(accessCode, csrf)).isFalse();
    SetupPasswordHelper.withRegisteredNumber(number);
    int numberOfSmsSendsRow = SetupPasswordHelper.countSendHistoryRecords();

    ResponseEntity<SendAccessTokenController.SendAccessCodeResponse> response =
        controller.sendAccessCode(input);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody().getError()).isNull();
    assertThat(response.getBody().getCsrf()).isEqualTo(csrf);
    assertThat(SetupPasswordHelper.countSendHistoryRecords()).isEqualTo(numberOfSmsSendsRow + 1);
    assertThat(SetupPasswordHelper.accessTokenExists(accessCode, csrf)).isTrue();
  }
}