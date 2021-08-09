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

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Represents a JSON array.
 */
@SdkInternalApi
public final class SdkArrayNode implements SdkJsonNode {

    private final List<SdkJsonNode> items;

    private SdkArrayNode(Builder builder) {
        this.items = unmodifiableList(new ArrayList<>(builder.items));
    }

    @Override
    public List<SdkJsonNode> items() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdkArrayNode that = (SdkArrayNode) o;
        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(items);
    }

    @Override
    public String toString() {
        return items.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",", "[", "]"));
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {

        private final List<SdkJsonNode> items = new ArrayList<>();

        private Builder() {
        }

        Builder addItem(SdkJsonNode item) {
            this.items.add(item);
            return this;
        }

        SdkArrayNode build() {
            return new SdkArrayNode(this);
        }
    }
}
