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

package software.amazon.awssdk.enhanced.dynamodb.query.engine;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Converts DynamoDB {@link AttributeValue} and maps of them to Java types for condition evaluation and row building in the
 * complex query engine.
 */
@SdkInternalApi
final class AttributeValueConversion {

    private AttributeValueConversion() {
    }

    /**
     * Converts a single AttributeValue to a Java object suitable for comparison and display. Numbers are returned as BigDecimal
     * for consistent ordering.
     */
    static Object toObject(AttributeValue av) {
        if (av == null) {
            return null;
        }
        if (av.nul() != null && av.nul()) {
            return null;
        }
        if (av.s() != null) {
            return av.s();
        }
        if (av.n() != null) {
            return new BigDecimal(av.n());
        }
        if (av.bool() != null) {
            return av.bool();
        }
        if (av.b() != null) {
            return av.b();
        }
        if (av.m() != null) {
            Map<String, Object> map = new HashMap<>();
            av.m().forEach((k, v) -> map.put(k, toObject(v)));
            return map;
        }
        if (av.l() != null) {
            return av.l().stream().map(AttributeValueConversion::toObject).collect(Collectors.toList());
        }
        if (av.ss() != null) {
            return av.ss();
        }
        if (av.ns() != null) {
            return av.ns().stream().map(BigDecimal::new).collect(Collectors.toSet());
        }
        if (av.bs() != null) {
            return av.bs();
        }
        return null;
    }

    /**
     * Converts a map of attribute name to AttributeValue into a map of attribute name to Java object.
     */
    static Map<String, Object> toObjectMap(Map<String, AttributeValue> attributeMap) {
        if (attributeMap == null || attributeMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        attributeMap.forEach((k, v) -> result.put(k, toObject(v)));
        return result;
    }

    /**
     * Converts a join key object (typically String or Number) into an AttributeValue suitable for use in low-level Query or Scan
     * requests.
     */
    static AttributeValue toKeyAttributeValue(Object key) {
        if (key == null) {
            return AttributeValue.builder().nul(true).build();
        }
        if (key instanceof String) {
            return AttributeValue.builder().s((String) key).build();
        }
        if (key instanceof Number) {
            return AttributeValue.builder().n(key.toString()).build();
        }
        return AttributeValue.builder().s(key.toString()).build();
    }
}
