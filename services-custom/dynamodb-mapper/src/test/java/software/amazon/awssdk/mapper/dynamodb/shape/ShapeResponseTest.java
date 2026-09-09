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

package software.amazon.awssdk.mapper.dynamodb.shape;

import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.HASH_KEY;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.bool;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.entry;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.item;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.l;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.m;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.n;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.ns;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.s;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.ss;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.verify;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.AllTypesItem;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.StringItem;

// Reconstructs POJOs from canned attribute maps via marshallIntoObject and asserts the result against a fixture.
@RunWith(Parameterized.class)
public class ShapeResponseTest {

    private static final String FIXTURE = "unmarshall_item_fixture.json";
    private static final DynamoDBMapper MAPPER = new DynamoDBMapper((DynamoDbClient) null);

    // NON_NULL keeps the fixture to the reconstructed attributes; alphabetical sort pins the reflection-ordered fields.
    private static final ObjectMapper READ_JSON = new ObjectMapper()
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .setSerializationInclusion(Include.NON_NULL);

    static final class Case {
        final String name;
        final Class<?> type;
        final Map<String, AttributeValue> attributes;

        Case(String name, Class<?> type, Map<String, AttributeValue> attributes) {
            this.name = name;
            this.type = type;
            this.attributes = attributes;
        }

        @Override
        public String toString() {
            return "unmarshall:" + name;
        }
    }

    @Parameters(name = "{0}")
    public static List<Case> cases() {
        List<Case> c = new ArrayList<>();

        c.add(new Case("string", StringItem.class, item("value", s("hello"))));
        c.add(new Case("string-unicode", StringItem.class, item("value", s("héllo-世界-😀"))));
        c.add(new Case("number", AllTypesItem.class, item("number", n("42"))));
        c.add(new Case("number-negative", AllTypesItem.class, item("number", n("-7"))));
        c.add(new Case("double", AllTypesItem.class, item("double", n("1.5"))));
        c.add(new Case("bool-numeric-true", AllTypesItem.class, item("numericBool", n("1"))));
        c.add(new Case("bool-numeric-false", AllTypesItem.class, item("numericBool", n("0"))));
        c.add(new Case("bool-native-true", AllTypesItem.class, item("nativeBool", bool(true))));
        c.add(new Case("bool-native-false", AllTypesItem.class, item("nativeBool", bool(false))));
        c.add(new Case("string-set", AllTypesItem.class, item("stringSet", ss("a", "b", "c"))));
        c.add(new Case("number-set", AllTypesItem.class, item("numberSet", ns("1", "2", "3"))));
        c.add(new Case("list", AllTypesItem.class, item("list", l(s("first"), s("second")))));
        c.add(new Case("map", AllTypesItem.class, item("map", m(entry("a", s("1")), entry("b", s("2"))))));
        c.add(new Case("document", AllTypesItem.class,
            item("document", m(entry("city", s("Seattle")), entry("zip", n("98101"))))));
        c.add(new Case("enum", AllTypesItem.class, item("enum", s("GREEN"))));

        return c;
    }

    @Parameter
    public Case testCase;

    @Test
    public void matchesFixture() {
        verify(FIXTURE, testCase.name, reconstruct(testCase.type, testCase.attributes));
    }

    private static <T> String reconstruct(Class<T> type, Map<String, AttributeValue> attributes) {
        T object = MAPPER.marshallIntoObject(type, attributes, DynamoDBMapperConfig.DEFAULT);
        try {
            return READ_JSON.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize reconstructed " + type.getSimpleName(), e);
        }
    }
}
