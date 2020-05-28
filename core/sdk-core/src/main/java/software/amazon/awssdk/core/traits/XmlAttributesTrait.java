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

package software.amazon.awssdk.core.traits;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Pair;

/**
 * Trait to include the xml attributes such as "xmlns:xsi" or "xsi:type".
 */
@SdkProtectedApi
public final class XmlAttributesTrait implements Trait {
    private Map<String, AttributeAccessors> attributes;

    private XmlAttributesTrait(Pair<String, AttributeAccessors>... attributePairs) {
        attributes = new LinkedHashMap<>();
        for (Pair<String, AttributeAccessors> pair : attributePairs) {
            attributes.put(pair.left(), pair.right());
        }
        attributes = Collections.unmodifiableMap(attributes);
    }

    public static XmlAttributesTrait create(Pair<String, AttributeAccessors>... pairs) {
        return new XmlAttributesTrait(pairs);
    }

    public Map<String, AttributeAccessors> attributes() {
        return attributes;
    }

    public static final class AttributeAccessors {
        private final Function<Object, String> attributeGetter;

        private AttributeAccessors(Builder builder) {
            this.attributeGetter = builder.attributeGetter;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * @return the attribute Getter method
         */
        public Function<Object, String> attributeGetter() {
            return attributeGetter;
        }

        public static final class Builder {
            private Function<Object, String> attributeGetter;

            private Builder() {
            }

            public Builder attributeGetter(Function<Object, String> attributeGetter) {
                this.attributeGetter = attributeGetter;
                return this;
            }

            public AttributeAccessors build() {
                return new AttributeAccessors(this);
            }

        }
    }
}
