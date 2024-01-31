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

package software.amazon.awssdk.core.protocol;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Dummy implementation of {@link SdkResponse}.
 */
@SdkProtectedApi
public final class VoidSdkResponse extends SdkResponse {

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Collections.emptyList());

    private VoidSdkResponse(Builder builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return builder();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    public static final class Builder extends BuilderImpl implements SdkPojo, SdkBuilder<Builder, SdkResponse> {

        private Builder() {
        }

        @Override
        public SdkResponse build() {
            return new VoidSdkResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

    }
}
