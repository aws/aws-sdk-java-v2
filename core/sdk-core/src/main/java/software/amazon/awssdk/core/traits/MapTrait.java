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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkField;

/**
 * Trait that includes additional metadata for Map members.
 */
@SdkProtectedApi
public final class MapTrait implements Trait {

    private final String keyLocationName;
    private final String valueLocationName;
    private final SdkField valueFieldInfo;
    private final boolean isFlattened;

    private MapTrait(Builder builder) {
        this.keyLocationName = builder.keyLocationName;
        this.valueLocationName = builder.valueLocationName;
        this.valueFieldInfo = builder.valueFieldInfo;
        this.isFlattened = builder.isFlattened;
    }

    /**
     * @return Location name of key. Used only for XML based protocols.
     */
    public String keyLocationName() {
        return keyLocationName;
    }

    /**
     * @return Location name of value. Used only for XML based protocols.
     */
    public String valueLocationName() {
        return valueLocationName;
    }

    /**
     * @return Additional metadata for the map value types. May be further nested in the case of complex containers.
     */
    public SdkField valueFieldInfo() {
        return valueFieldInfo;
    }

    /**
     * @return Whether the map should be marshalled/unmarshalled as a 'flattened' map. This only applies to Query/XML protocols.
     */
    public boolean isFlattened() {
        return isFlattened;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String keyLocationName;
        private String valueLocationName;
        private SdkField valueFieldInfo;
        private boolean isFlattened;

        private Builder() {
        }

        public Builder keyLocationName(String keyLocationName) {
            this.keyLocationName = keyLocationName;
            return this;
        }

        public Builder valueLocationName(String valueLocationName) {
            this.valueLocationName = valueLocationName;
            return this;
        }

        public Builder valueFieldInfo(SdkField valueFieldInfo) {
            this.valueFieldInfo = valueFieldInfo;
            return this;
        }

        public Builder isFlattened(boolean isFlattened) {
            this.isFlattened = isFlattened;
            return this;
        }

        public MapTrait build() {
            return new MapTrait(this);
        }
    }
}
