/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromListOfAttributeValues;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromMap;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromString;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.AnnotatedBeanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.Item;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.ImmutableMap;

public class AnnotatedBeanAttributeConvertersTest {
    private static final AnnotatedBeanAttributeConverter CONVERTER = AnnotatedBeanAttributeConverter.create();
    private static final ItemAttributeValue EMPTY_IAV = fromMap(emptyMap());

    @Test
    public void simpleItemConversionWorks() {
        SimpleItem item = new SimpleItem();
        item.setId("foo");

        ItemAttributeValue iavItem = fromMap(ImmutableMap.of("id", fromString("foo")));

        // Make a few attempts to check the cold and less cold paths
        assertThat(CONVERTER.toAttributeValue(item)).isEqualTo(iavItem);
        assertThat(CONVERTER.toAttributeValue(item)).isEqualTo(iavItem);
        assertThat(CONVERTER.toAttributeValue(item)).isEqualTo(iavItem);

        assertThat(CONVERTER.fromAttributeValue(iavItem, TypeToken.of(SimpleItem.class))).isEqualToComparingFieldByField(item);
    }

    @Test
    public void emptyItemConversionWorks() {
        assertThat(CONVERTER.toAttributeValue(new EmptyItem())).isEqualTo(EMPTY_IAV);
    }

    @Test
    public void missingItemAnnotationFails() {
        assertFails(() -> CONVERTER.toAttributeValue(new MissingAnnotationItem()));
        assertFails(() -> CONVERTER.fromAttributeValue(EMPTY_IAV, TypeToken.of(MissingAnnotationItem.class)));
    }

    @Test
    public void nonStaticInnerClassFails() {
        assertFails(() -> CONVERTER.toAttributeValue(new NonStaticInnerClass()));
    }

    @Test
    public void privateConstructorClassFails() {
        assertFails(() -> CONVERTER.toAttributeValue(new PrivateConstructorClass()));
    }

    @Test
    public void argedConstructorClassFails() {
        assertFails(() -> CONVERTER.toAttributeValue(new MissingZeroArgConstructorClass(1)));
    }

    @Test
    public void duplicateGetterClassFails() {
        assertFails(() -> CONVERTER.toAttributeValue(new DuplicateGetterItem()));
    }

    @Test
    public void duplicateSetterClassFails() {
        assertFails(() -> CONVERTER.toAttributeValue(new DuplicateSetterItem()));
    }

    @Test
    public void localClassFails() {
        @Item class LocalItem {}
        assertFails(() -> CONVERTER.toAttributeValue(new LocalItem()));
    }

    @Test
    public void missingCollectionAnnotationFails() {
        ItemAttributeValue map = fromMap(ImmutableMap.of("id", fromListOfAttributeValues(fromString("foo"))));
        assertFails(() -> CONVERTER.fromAttributeValue(map, TypeToken.of(CollectionWithoutAnnotation.class)));
    }

    @Item
    public static class SimpleItem {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @Item
    public static class CollectionWithoutAnnotation {
        private List<String> id;

        public List<String> getId() {
            return id;
        }

        public void setId(List<String> id) {
            this.id = id;
        }
    }

    @Item
    public static class EmptyItem {}

    public static class MissingAnnotationItem {}

    @Item
    public class NonStaticInnerClass {}

    @Item
    public static class PrivateConstructorClass {
        private PrivateConstructorClass() {}
    }

    @Item
    public static class MissingZeroArgConstructorClass {
        public MissingZeroArgConstructorClass(int i) {}
    }

    @Item
    public static class DuplicateGetterItem {
        private String id;

        public String getId() {
            return id;
        }

        public String isId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @Item
    public static class DuplicateSetterItem {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setId(UUID id) {
            this.id = id.toString();
        }
    }
}
