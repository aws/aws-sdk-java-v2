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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ResolvedStaticAttribute;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class StaticAttributeTest {
    private static final Function<Object, String> TEST_GETTER = x -> "test-getter";
    private static final BiConsumer<Object, String> TEST_SETTER = (x, y) -> {};

    private static class SimpleItem {
        private String aString;

        SimpleItem() {
        }

        SimpleItem(String aString) {
            this.aString = aString;
        }

        String getAString() {
            return this.aString;
        }

        void setAString(String aString) {
            this.aString = aString;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SimpleItem that = (SimpleItem) o;
            return aString == that.aString;
        }

        @Override
        public int hashCode() {
            return Objects.hash(aString);
        }
    }

    @Mock
    private StaticAttributeTag mockTag;

    @Mock
    private StaticAttributeTag mockTag2;

    @Mock
    private AttributeConverter<String> attributeConverter;

    @Test
    public void build_maximal() {
        StaticAttribute<Object, String> staticAttribute = StaticAttribute.builder(Object.class, String.class)
                                                                         .name("test-attribute")
                                                                         .getter(TEST_GETTER)
                                                                         .setter(TEST_SETTER)
                                                                         .tags(mockTag)
                                                                         .build();

        assertThat(staticAttribute.name()).isEqualTo("test-attribute");
        assertThat(staticAttribute.getter()).isSameAs(TEST_GETTER);
        assertThat(staticAttribute.setter()).isSameAs(TEST_SETTER);
        assertThat(staticAttribute.tags()).containsExactly(mockTag);
        assertThat(staticAttribute.type()).isEqualTo(EnhancedType.of(String.class));
    }

    @Test
    public void build_minimal() {
        StaticAttribute<Object, String> staticAttribute = StaticAttribute.builder(Object.class, String.class)
                                                                         .name("test-attribute")
                                                                         .getter(TEST_GETTER)
                                                                         .setter(TEST_SETTER)
                                                                         .build();

        assertThat(staticAttribute.name()).isEqualTo("test-attribute");
        assertThat(staticAttribute.getter()).isSameAs(TEST_GETTER);
        assertThat(staticAttribute.setter()).isSameAs(TEST_SETTER);
        assertThat(staticAttribute.tags()).isEmpty();
        assertThat(staticAttribute.type()).isEqualTo(EnhancedType.of(String.class));
    }

    @Test
    public void build_missing_name() {
        assertThatThrownBy(() -> StaticAttribute.builder(Object.class, String.class)
                                                .getter(TEST_GETTER)
                                                .setter(TEST_SETTER)
                                                .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    public void build_missing_getter() {
        assertThatThrownBy(() -> StaticAttribute.builder(Object.class, String.class)
                                                .name("test-attribute")
                                                .setter(TEST_SETTER)
                                                .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("getter");
    }

    @Test
    public void build_missing_setter() {
        assertThatThrownBy(() -> StaticAttribute.builder(Object.class, String.class)
                                                .name("test-attribute")
                                                .getter(TEST_GETTER)
                                                .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("setter");
    }

    @Test
    public void toBuilder() {
        StaticAttribute<Object, String> staticAttribute = StaticAttribute.builder(Object.class, String.class)
                                                                         .name("test-attribute")
                                                                         .getter(TEST_GETTER)
                                                                         .setter(TEST_SETTER)
                                                                         .tags(mockTag, mockTag2)
                                                                         .build();

        StaticAttribute<Object, String> clonedAttribute = staticAttribute.toBuilder().build();

        assertThat(clonedAttribute.name()).isEqualTo("test-attribute");
        assertThat(clonedAttribute.getter()).isSameAs(TEST_GETTER);
        assertThat(clonedAttribute.setter()).isSameAs(TEST_SETTER);
        assertThat(clonedAttribute.tags()).containsExactly(mockTag, mockTag2);
        assertThat(clonedAttribute.type()).isEqualTo(EnhancedType.of(String.class));
    }

    @Test
    public void build_addTag_single() {
        StaticAttribute<Object, String> staticAttribute = StaticAttribute.builder(Object.class, String.class)
                                                                         .name("test-attribute")
                                                                         .getter(TEST_GETTER)
                                                                         .setter(TEST_SETTER)
                                                                         .addTag(mockTag)
                                                                         .build();

        assertThat(staticAttribute.tags()).containsExactly(mockTag);
    }

    @Test
    public void build_addTag_multiple() {
        StaticAttribute<Object, String> staticAttribute = StaticAttribute.builder(Object.class, String.class)
                                                                         .name("test-attribute")
                                                                         .getter(TEST_GETTER)
                                                                         .setter(TEST_SETTER)
                                                                         .addTag(mockTag)
                                                                         .addTag(mockTag2)
                                                                         .build();

        assertThat(staticAttribute.tags()).containsExactly(mockTag, mockTag2);
    }

    @Test
    public void build_addAttributeConverter() {
        StaticAttribute<Object, String> staticAttribute = StaticAttribute.builder(Object.class, String.class)
                                                                         .name("test-attribute")
                                                                         .getter(TEST_GETTER)
                                                                         .setter(TEST_SETTER)
                                                                         .attributeConverter(attributeConverter)
                                                                         .build();

        AttributeConverter<String> attributeConverterR = staticAttribute.attributeConverter();
        assertThat(attributeConverterR).isEqualTo(attributeConverter);
    }

    @Test
    public void resolve_uses_customConverter() {
        when(attributeConverter.transformFrom(any())).thenReturn(AttributeValue.builder().s("test-string-custom").build());

        StaticAttribute<SimpleItem, String> staticAttribute = StaticAttribute.builder(SimpleItem.class, String.class)
                                                                         .name("test-attribute")
                                                                         .getter(SimpleItem::getAString)
                                                                         .setter(SimpleItem::setAString)
                                                                         .attributeConverter(attributeConverter)
                                                                         .build();

        ResolvedStaticAttribute<SimpleItem> resolvedAttribute =
            staticAttribute.resolve(AttributeConverterProvider.defaultProvider());

        Function<SimpleItem, AttributeValue> attributeValueFunction = resolvedAttribute.attributeGetterMethod();

        SimpleItem item = new SimpleItem("test-string");
        AttributeValue resultAttributeValue = attributeValueFunction.apply(item);

        assertThat(resultAttributeValue.s()).isEqualTo("test-string-custom");
    }

}