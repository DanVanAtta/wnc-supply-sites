package com.vanatta.helene.supplies.database.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneNumberUtilTest {

  @CsvSource(value = {"a,''", "123a,123", "(555) 333-1213,5553331213", " 123 ,123"})
  @ParameterizedTest
  void removeNonNumeric(String input, String output) {
    assertThat(PhoneNumberUtil.removeNonNumeric(input)).isEqualTo(output);
  }

  @CsvSource(
      value = {
        ",false",
        "123a,false",
        "(555) 333-121,false",
        "(555) 333-121x,false",
        "(555) 333-1213,true",
        "555.333.1213,true",
        "555-333-1213,true",
        "(555) 333-12134,false",
      })
  @ParameterizedTest
  void isValid(String input, boolean output) {
    assertThat(PhoneNumberUtil.isValid(input)).isEqualTo(output);
  }
}
