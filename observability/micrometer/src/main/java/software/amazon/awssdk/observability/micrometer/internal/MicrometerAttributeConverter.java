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

package software.amazon.awssdk.observability.micrometer.internal;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.observability.attributes.Attributes;

public final class MicrometerAttributeConverter {

    private MicrometerAttributeConverter() {
    }

    /**
     * Converts SDK Attributes to Micrometer KeyValues for use with Observation API.
     *
     * @param attributes The SDK attributes to convert
     * @return An iterable of KeyValue objects suitable for Micrometer Observation
     */
    public static Iterable<KeyValue> toKeyValues(Attributes attributes) {
        if (attributes == null) {
            return Collections.emptyList();
        }

        List<KeyValue> keyValues = new ArrayList<>(attributes.size());
        attributes.forEach((key, value) -> keyValues.add(KeyValue.of(key, convertValueToString(value))));
        return keyValues;
    }

    /**
     * Converts SDK Attributes to Micrometer Tags for use with meter registry.
     *
     * @param attributes The SDK attributes to convert
     * @return Tags object suitable for Micrometer metrics
     */
    public static Tags toMicrometerTags(Attributes attributes) {
        if (attributes == null) {
            return Tags.empty();
        }

        List<Tag> tags = new ArrayList<>(attributes.size());
        attributes.forEach((key, value) -> tags.add(Tag.of(key, convertValueToString(value))));
        return Tags.of(tags);
    }

    /**
     * Converts Micrometer KeyValues back to SDK Attributes.
     *
     * @param keyValues The Micrometer KeyValues to convert
     * @return SDK Attributes object
     */
    public static Attributes fromKeyValues(Iterable<KeyValue> keyValues) {
        if (keyValues == null) {
            return Attributes.builder().build();
        }

        Attributes.Builder builder = Attributes.builder();
        for (KeyValue kv : keyValues) {
            builder.put(kv.getKey(), kv.getValue());
        }
        return builder.build();
    }

    /**
     * Converts Micrometer Tags back to SDK Attributes.
     *
     * @param tags The Micrometer Tags to convert
     * @return SDK Attributes object
     */
    public static Attributes fromMicrometerTags(Tags tags) {
        if (tags == null) {
            return Attributes.builder().build();
        }

        Attributes.Builder builder = Attributes.builder();
        tags.forEach(tag -> builder.put(tag.getKey(), tag.getValue()));
        return builder.build();
    }

    /**
     * Converts a value object to its string representation.
     * Handles special cases like lists.
     */
    private static String convertValueToString(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof List) {
            return ((Collection<?>) value).stream()
                                          .map(Object::toString)
                                          .collect(Collectors.joining(","));
        }

        return value.toString();
    }
}
