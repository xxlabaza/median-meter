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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled
@ExtendWith(MockitoExtension.class)
class MockitoTests {

  @Nested
  @DisplayName("@Mock Annotation")
  class MockAnnotation {

    @Mock
    List<String> mockedList;

    @Test
    void whenUseMockAnnotation_thenMockIsInjected () {
      mockedList.add("one");
      verify(mockedList).add("one");
      assertEquals(0, mockedList.size());

      when(mockedList.size()).thenReturn(100);
      assertEquals(100, mockedList.size());
    }

    @Test
    void whenUseMockAnnotation_thenMockIsInjectedAsArgument (@Mock List<String> list) {
      list.add("one");
      verify(list).add("one");
      assertEquals(0, list.size());

      when(list.size()).thenReturn(100);
      assertEquals(100, list.size());
    }

    @Test
    void whenNotUseMockAnnotation_thenCorrect () {
      List<String> mockList = mock(StringArrayList.class);

      mockList.add("one");
      verify(mockList).add("one");
      assertEquals(0, mockList.size());

      when(mockList.size()).thenReturn(100);
      assertEquals(100, mockList.size());
    }
  }

  @Nested
  @DisplayName("@Spy Annotation")
  class SpyAnnotation {

    @Spy
    List<String> spiedList = new ArrayList<String>();

    @Test
    void whenUseSpyAnnotation_thenSpyIsInjected () {
      spiedList.add("one");
      spiedList.add("two");

      verify(spiedList).add("one");
      verify(spiedList).add("two");

      assertEquals(2, spiedList.size());

      doReturn(100).when(spiedList).size();
      assertEquals(100, spiedList.size());
    }

    @Test
    void whenNotUseSpyAnnotation_thenCorrect () {
      List<String> spyList = spy(new ArrayList<String>());

      spyList.add("one");
      spyList.add("two");

      verify(spyList).add("one");
      verify(spyList).add("two");

      assertEquals(2, spyList.size());

      doReturn(100).when(spyList).size();
      assertEquals(100, spyList.size());
    }
  }

  @Nested
  @DisplayName("@Captor Annotation")
  class CaptorAnnotation {

    @Mock
    List<String> mockedList;

    @Captor
    ArgumentCaptor<String> argCaptor;

    @Test
    void whenUseCaptorAnnotation_thenTheSam () {
      mockedList.add("one");
      verify(mockedList).add(argCaptor.capture());

      assertEquals("one", argCaptor.getValue());
    }

    @Test
    void whenNotUseCaptorAnnotation_thenCorrect () {
      List<String> mockList = mock(StringList.class);
      ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);

      mockList.add("one");
      verify(mockList).add(arg.capture());

      assertEquals("one", arg.getValue());
    }
  }

  @Nested
  @DisplayName("@InjectMocks Annotation")
  class InjectMocksAnnotation {

    @Mock
    Map<String, String> wordMap;

    @InjectMocks
    MyDictionary dic = new MyDictionary();

    @Test
    void whenUseInjectMocksAnnotation_thenCorrect () {
      when(wordMap.get("aWord")).thenReturn("aMeaning");

      assertEquals("aMeaning", dic.getMeaning("aWord"));
    }

    class MyDictionary {

      Map<String, String> wordMap;

      MyDictionary () {
        wordMap = new HashMap<String, String>();
      }

      void add (String word, String meaning) {
        wordMap.put(word, meaning);
      }

      String getMeaning (String word) {
        return wordMap.get(word);
      }
    }
  }

  class StringArrayList extends ArrayList<String> {

    private static final long serialVersionUID = 1L;
  }

  interface StringList extends List<String> {

  }
}
