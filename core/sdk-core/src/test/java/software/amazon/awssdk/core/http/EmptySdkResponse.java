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

package software.amazon.awssdk.core.http;


import software.amazon.awssdk.core.SdkResponse;

public class EmptySdkResponse extends SdkResponse {

    private EmptySdkResponse(Builder builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkResponse.Builder {
        @Override
        EmptySdkResponse build();
    }

    private static class BuilderImpl extends SdkResponse.BuilderImpl implements Builder {

        @Override
        public EmptySdkResponse build() {
            return new EmptySdkResponse(this);
        }
    }
}
