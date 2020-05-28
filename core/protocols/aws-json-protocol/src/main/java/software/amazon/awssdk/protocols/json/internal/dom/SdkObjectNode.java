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

package software.amazon.awssdk.protocols.json.internal.dom;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class SdkObjectNode implements SdkJsonNode {

    private static final SdkObjectNode EMPTY = SdkObjectNode.builder().build();

    private final Map<String, SdkJsonNode> fields;

    private SdkObjectNode(Builder builder) {
        this.fields = unmodifiableMap(new HashMap<>(builder.fields));
    }

    @Override
    public SdkJsonNode get(String fieldName) {
        return fields.get(fieldName);
    }

    @Override
    public Map<String, SdkJsonNode> fields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdkObjectNode that = (SdkObjectNode) o;
        return Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
    }

    @Override
    public String toString() {
        return fields.entrySet().stream()
                     .map(e -> String.format("\"%s\": %s", e.getKey(), e.getValue()))
                     .collect(Collectors.joining(",\n", "{\n", "\n}"));
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * @return An empty JSON object.
     */
    public static SdkObjectNode emptyObject() {
        return EMPTY;
    }

    static final class Builder {

        private final Map<String, SdkJsonNode> fields = new HashMap<>();

        private Builder() {
        }

        Builder putField(String fieldName, SdkJsonNode value) {
            fields.put(fieldName, value);
            return this;
        }

        SdkObjectNode build() {
            return new SdkObjectNode(this);
        }
    }
}
