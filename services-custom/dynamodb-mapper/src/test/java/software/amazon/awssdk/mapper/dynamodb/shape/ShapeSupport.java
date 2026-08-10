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

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.IoUtils;

// Fixture assertion and AttributeValue builders shared by ShapeRequestTest and ShapeResponseTest.
final class ShapeSupport {

    static final String HASH_KEY = "k";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> ENTRIES = new TypeReference<Map<String, String>>() {
    };
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    private ShapeSupport() {
    }

    // Asserts actual equals the committed fixture entry name in fixtureFile.
    static void verify(String fixtureFile, String name, String actual) {
        String expected = loadFixture(fixtureFile).get(name);
        if (expected == null) {
            throw new IllegalStateException("No fixture entry '" + name + "' in " + fixtureFile);
        }
        assertEquals("Shape drifted for " + fixtureFile + ":" + name, expected, actual);
    }

    private static Map<String, String> loadFixture(String fixtureFile) {
        return CACHE.computeIfAbsent(fixtureFile, file -> {
            try (InputStream in = ShapeSupport.class.getClassLoader().getResourceAsStream(file)) {
                if (in == null) {
                    throw new IllegalStateException("Missing fixture " + file);
                }
                return JSON.readValue(IoUtils.toUtf8String(in), ENTRIES);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    // A key/response map of hash key id=k plus one named attribute.
    static Map<String, AttributeValue> item(String attr, AttributeValue value) {
        Map<String, AttributeValue> map = new LinkedHashMap<>();
        map.put("id", s(HASH_KEY));
        map.put(attr, value);
        return map;
    }

    static AttributeValue s(String v) {
        return AttributeValue.builder().s(v).build();
    }

    static AttributeValue n(String v) {
        return AttributeValue.builder().n(v).build();
    }

    static AttributeValue bool(boolean v) {
        return AttributeValue.builder().bool(v).build();
    }

    static AttributeValue ss(String... v) {
        return AttributeValue.builder().ss(v).build();
    }

    static AttributeValue ns(String... v) {
        return AttributeValue.builder().ns(v).build();
    }

    static AttributeValue l(AttributeValue... v) {
        return AttributeValue.builder().l(v).build();
    }

    @SafeVarargs
    static AttributeValue m(Map.Entry<String, AttributeValue>... entries) {
        Map<String, AttributeValue> map = new LinkedHashMap<>();
        for (Map.Entry<String, AttributeValue> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return AttributeValue.builder().m(map).build();
    }

    static Map.Entry<String, AttributeValue> entry(String k, AttributeValue v) {
        return new java.util.AbstractMap.SimpleEntry<>(k, v);
    }
}
