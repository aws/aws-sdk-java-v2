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

package software.amazon.awssdk.awscore.client.http;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;

public class NoopTestAwsRequest extends AwsRequest {
    private NoopTestAwsRequest(Builder builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends AwsRequest.Builder {
        @Override
        NoopTestAwsRequest build();

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration awsRequestOverrideConfig);
    }

    private static class BuilderImpl extends AwsRequest.BuilderImpl implements Builder {

        @Override
        public NoopTestAwsRequest build() {
            return new NoopTestAwsRequest(this);
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration awsRequestOverrideConfig) {
            super.overrideConfiguration(awsRequestOverrideConfig);
            return this;
        }
    }
}
