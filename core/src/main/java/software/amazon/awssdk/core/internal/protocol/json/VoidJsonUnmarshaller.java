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

package software.amazon.awssdk.core.internal.protocol.json;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

/**
 * Simple unmarshaller that iterates through the JSON events but always
 * returns a dummy empty {@link SdkResponse}.
 */
@SdkInternalApi
public class VoidJsonUnmarshaller implements Unmarshaller<SdkResponse, JsonUnmarshallerContext> {

    public SdkResponse unmarshall(JsonUnmarshallerContext context) throws Exception {
        return EmptySdkResponse.builder().build();
    }

    /**
     * Dummy implementation of {@link SdkResponse}.
     */
    private static final class EmptySdkResponse extends SdkResponse {

        EmptySdkResponse(Builder builder) {
            super(builder);
        }

        @Override
        public Builder toBuilder() {
            return builder();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder extends SdkResponse.BuilderImpl {

            private Builder() {
            }

            @Override
            public SdkResponse build() {
                return new EmptySdkResponse(this);
            }
        }
    }
}
