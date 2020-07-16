/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;

import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class EnhancedTypeTest {
    @Test
    public void anonymousCreationCapturesComplexTypeArguments() {
        EnhancedType<Map<String, List<List<String>>>> enhancedType = new EnhancedType<Map<String, List<List<String>>>>(){};
        assertThat(enhancedType.rawClass()).isEqualTo(Map.class);
        assertThat(enhancedType.rawClassParameters().get(0).rawClass()).isEqualTo(String.class);
        assertThat(enhancedType.rawClassParameters().get(1).rawClass()).isEqualTo(List.class);
        assertThat(enhancedType.rawClassParameters().get(1).rawClassParameters().get(0).rawClass()).isEqualTo(List.class);
        assertThat(enhancedType.rawClassParameters().get(1).rawClassParameters().get(0).rawClassParameters().get(0).rawClass())
            .isEqualTo(String.class);
    }

    @Test
    public void customTypesWork() {
        EnhancedType<EnhancedTypeTest> enhancedType = new EnhancedType<EnhancedTypeTest>(){};
        assertThat(enhancedType.rawClass()).isEqualTo(EnhancedTypeTest.class);
    }

    @Test
    public void nonStaticInnerTypesWork() {
        EnhancedType<InnerType> enhancedType = new EnhancedType<InnerType>(){};
        assertThat(enhancedType.rawClass()).isEqualTo(InnerType.class);
    }

    @Test
    public void staticInnerTypesWork() {
        EnhancedType<InnerStaticType> enhancedType = new EnhancedType<InnerStaticType>(){};
        assertThat(enhancedType.rawClass()).isEqualTo(InnerStaticType.class);
    }

    @Test
    public <T> void genericParameterTypesDontWork() {
        assertThatThrownBy(() -> new EnhancedType<List<T>>(){}).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void helperCreationMethodsWork() {
        assertThat(EnhancedType.of(String.class).rawClass()).isEqualTo(String.class);

        assertThat(EnhancedType.listOf(String.class)).satisfies(v -> {
            assertThat(v.rawClass()).isEqualTo(List.class);
            assertThat(v.rawClassParameters()).hasSize(1);
            assertThat(v.rawClassParameters().get(0).rawClass()).isEqualTo(String.class);
        });

        assertThat(EnhancedType.mapOf(String.class, Integer.class)).satisfies(v -> {
            assertThat(v.rawClass()).isEqualTo(Map.class);
            assertThat(v.rawClassParameters()).hasSize(2);
            assertThat(v.rawClassParameters().get(0).rawClass()).isEqualTo(String.class);
            assertThat(v.rawClassParameters().get(1).rawClass()).isEqualTo(Integer.class);
        });
    }

    @Test
    public void equalityIsBasedOnInnerEquality() {
        assertThat(EnhancedType.of(String.class)).isEqualTo(EnhancedType.of(String.class));
        assertThat(EnhancedType.of(String.class)).isNotEqualTo(EnhancedType.of(Integer.class));

        assertThat(new EnhancedType<Map<String, List<String>>>(){}).isEqualTo(new EnhancedType<Map<String, List<String>>>(){});
        assertThat(new EnhancedType<Map<String, List<String>>>(){}).isNotEqualTo(new EnhancedType<Map<String, List<Integer>>>(){});
    }

    @Test
    public void dequeOf_ReturnsRawClassOfDeque_WhenSpecifyingClass() {
        EnhancedType<Deque<String>> type = EnhancedType.dequeOf(String.class);

        assertThat(type.rawClass()).isEqualTo(Deque.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class));
    }

    @Test
    public void dequeOf_ReturnsRawClassOfDeque_WhenSpecifyingEnhancedType() {
        EnhancedType<Deque<String>> type = EnhancedType.dequeOf(EnhancedType.of(String.class));

        assertThat(type.rawClass()).isEqualTo(Deque.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class));
    }

    @Test
    public void sortedSetOf_ReturnsRawClassOfDeque_WhenSpecifyingClass() {
        EnhancedType<SortedSet<String>> type = EnhancedType.sortedSetOf(String.class);

        assertThat(type.rawClass()).isEqualTo(SortedSet.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class));
    }

    @Test
    public void sortedSetOf_ReturnsRawClassOfDeque_WhenSpecifyingEnhancedType() {
        EnhancedType<SortedSet<String>> type = EnhancedType.sortedSetOf(EnhancedType.of(String.class));

        assertThat(type.rawClass()).isEqualTo(SortedSet.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class));
    }

    @Test
    public void navigableSetOf_ReturnsRawClassOfNavigableSet_WhenSpecifyingClass() {
        EnhancedType<NavigableSet<String>> type = EnhancedType.navigableSetOf(String.class);

        assertThat(type.rawClass()).isEqualTo(NavigableSet.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class));
    }

    @Test
    public void navigableSetOf_ReturnsRawClassOfNavigableSet_WhenSpecifyingEnhancedType() {
        EnhancedType<NavigableSet<String>> type = EnhancedType.navigableSetOf(EnhancedType.of(String.class));

        assertThat(type.rawClass()).isEqualTo(NavigableSet.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class));
    }


    @Test
    public void collectionOf_ReturnsRawClassOfCollection_WhenSpecifyingClass() {
        EnhancedType<Collection<String>> type = EnhancedType.collectionOf(String.class);

        assertThat(type.rawClass()).isEqualTo(Collection.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class));
    }

    @Test
    public void collectionOf_ReturnsRawClassOfCollection_WhenSpecifyingEnhancedType() {
        EnhancedType<Collection<String>> type = EnhancedType.collectionOf(EnhancedType.of(String.class));

        assertThat(type.rawClass()).isEqualTo(Collection.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class));
    }

    @Test
    public void sortedMapOf_ReturnsRawClassOfSortedMap_WhenSpecifyingClass() {
        EnhancedType<SortedMap<String, Integer>> type = EnhancedType.sortedMapOf(String.class, Integer.class);

        assertThat(type.rawClass()).isEqualTo(SortedMap.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class), EnhancedType.of(Integer.class));
    }

    @Test
    public void sortedMapOf_ReturnsRawClassOfSortedMap_WhenSpecifyingEnhancedType() {
        EnhancedType<SortedMap<String, Integer>> type =
            EnhancedType.sortedMapOf(EnhancedType.of(String.class), EnhancedType.of(Integer.class));

        assertThat(type.rawClass()).isEqualTo(SortedMap.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class), EnhancedType.of(Integer.class));
    }

    @Test
    public void concurrentMapOf_ReturnsRawClassOfConcurrentMap_WhenSpecifyingClass() {
        EnhancedType<ConcurrentMap<String, Integer>> type = EnhancedType.concurrentMapOf(String.class, Integer.class);

        assertThat(type.rawClass()).isEqualTo(ConcurrentMap.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class), EnhancedType.of(Integer.class));
    }

    @Test
    public void concurrentMapOf_ReturnsRawClassOfConcurrentMap_WhenSpecifyingEnhancedType() {
        EnhancedType<ConcurrentMap<String, Integer>> type =
            EnhancedType.concurrentMapOf(EnhancedType.of(String.class), EnhancedType.of(Integer.class));

        assertThat(type.rawClass()).isEqualTo(ConcurrentMap.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class), EnhancedType.of(Integer.class));
    }

    @Test
    public void navigableMapOf_ReturnsRawClassOfNavigableMap_WhenSpecifyingClass() {
        EnhancedType<NavigableMap<String, Integer>> type = EnhancedType.navigableMapOf(String.class, Integer.class);

        assertThat(type.rawClass()).isEqualTo(NavigableMap.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class), EnhancedType.of(Integer.class));
    }

    @Test
    public void navigableMapOf_ReturnsRawClassOfNavigableMap_WhenSpecifyingEnhancedType() {
        EnhancedType<NavigableMap<String, Integer>> type =
            EnhancedType.navigableMapOf(EnhancedType.of(String.class), EnhancedType.of(Integer.class));

        assertThat(type.rawClass()).isEqualTo(NavigableMap.class);
        assertThat(type.rawClassParameters()).containsExactly(EnhancedType.of(String.class), EnhancedType.of(Integer.class));
    }
    
    @Test
    public void documentOf_toString_doesNotRaiseNPE() {
        TableSchema<String> tableSchema = StaticTableSchema.builder(String.class).build();
        EnhancedType<String> type = EnhancedType.documentOf(String.class, tableSchema);
        assertThatCode(() -> type.toString()).doesNotThrowAnyException();
    }

    public class InnerType {
    }

    public static class InnerStaticType {
    }
}