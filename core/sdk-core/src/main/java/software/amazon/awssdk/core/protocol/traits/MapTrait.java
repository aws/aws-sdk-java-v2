/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.protocol.traits;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.protocol.SdkField;

@SdkProtectedApi
public final class MapTrait implements Trait {

    private final String keyLocationName;
    private final String valueLocationName;
    private final SdkField valueFieldInfo;

    private MapTrait(Builder builder) {
        this.keyLocationName = builder.keyLocationName;
        this.valueLocationName = builder.valueLocationName;
        this.valueFieldInfo = builder.valueFieldInfo;
    }

    public String keyLocationName() {
        return keyLocationName;
    }

    public String valueLocationName() {
        return valueLocationName;
    }

    public SdkField valueFieldInfo() {
        return valueFieldInfo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String keyLocationName;
        private String valueLocationName;
        private SdkField valueFieldInfo;

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

        public MapTrait build() {
            return new MapTrait(this);
        }
    }
}
