/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xxlabaza.test.median.meter.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@Disabled
@DisplayName("JUnit5 tests samples")
class JUnit5Tests {

  @BeforeAll
  static void beforeAll () {
    System.out.println("@BeforeAll - executes once before all test methods in this class");
  }

  @AfterAll
  static void afterAll () {
    System.out.println("@AfterAll - executed after all test methods.");
  }

  @BeforeEach
  void beforeEach1 () {
    System.out.println("@BeforeEach - executes before each test method in this class");
  }

  @BeforeEach
  void beforeEach2 (TestInfo testInfo) {
    System.out.println(testInfo.getDisplayName());
  }

  @AfterEach
  void afterEach () {
    System.out.println("@AfterEach - executed after each test method.");
  }

  @Test
  @DisplayName("Single test successful")
  void testSingleSuccessTest () {
    System.out.println("Success");
  }

  @Test
  @Disabled("Not implemented yet")
  void testShowSomething () {
    System.out.println("impossible!!!");
  }

  @Test
  void shouldThrowException () {
    Throwable exception = assertThrows(UnsupportedOperationException.class, () -> {
      throw new UnsupportedOperationException("Not supported");
    });
    assertEquals(exception.getMessage(), "Not supported");
  }

  @Test
  @DisplayName("╯°□°）╯")
  void assertThrowsException () {
    String str = null;
    assertThrows(IllegalArgumentException.class, () -> {
      Integer.valueOf(str);
    });
  }

  @RepeatedTest(3)
  void repeatedTestWithRepetitionInfo (RepetitionInfo repetitionInfo) {
    System.out.println("Repetition #" + repetitionInfo.getCurrentRepetition());
    assertEquals(3, repetitionInfo.getTotalRepetitions());
  }

  @TestFactory
  public Stream<DynamicTest> translateDynamicTestsFromStream () {
    return Stream.of(
            "Anna",
            "Civic",
            "Kayak",
            "Radar",
            "Able was I ere I saw Elba"
        )
        .map(it -> dynamicTest("Check polindrome '" + it + "'", () -> {
          String revert = new StringBuilder(it).reverse().toString();
          assertTrue(it.equalsIgnoreCase(revert));
        })
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {"Hello", "World"})
  @DisplayName("Should pass a non-null message to our test method")
  void shouldPassNonNullMessageAsMethodParameter (String message) {
    assertNotNull(message);
  }

  enum Pet {
    CAT,
    DOG;
  }

  @ParameterizedTest(name = "{index} => pet=''{0}''")
  @EnumSource(Pet.class)
  @DisplayName("Should pass non-null enum values as method parameters")
  void shouldPassNonNullEnumValuesAsMethodParameter (Pet pet) {
    assertNotNull(pet);
  }

  @ParameterizedTest(name = "{index} => a={0}, b={1}, sum={2}")
  @CsvSource({
      "1, 1, 2",
      "2, 3, 5"
  })
  @DisplayName("Should calculate the correct sum")
  void sum (int a, int b, int sum) {
    assertEquals(sum, a + b);
  }
}
