/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;

@Generated("software.amazon.awssdk:codegen")
public abstract class AcmResponse extends AwsResponse {
    private final AcmResponseMetadata responseMetadata;

    protected AcmResponse(Builder builder) {
        super(builder);
        this.responseMetadata = builder.responseMetadata();
    }

    @Override
    public AcmResponseMetadata responseMetadata() {
        return responseMetadata;
    }

    public interface Builder extends AwsResponse.Builder {
        @Override
        AcmResponse build();

        @Override
        AcmResponseMetadata responseMetadata();

        @Override
        Builder responseMetadata(AwsResponseMetadata metadata);
    }

    protected abstract static class BuilderImpl extends AwsResponse.BuilderImpl implements Builder {
        private AcmResponseMetadata responseMetadata;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AcmResponse response) {
            super(response);
            this.responseMetadata = response.responseMetadata();
        }

        @Override
        public AcmResponseMetadata responseMetadata() {
            return responseMetadata;
        }

        @Override
        public Builder responseMetadata(AwsResponseMetadata responseMetadata) {
            this.responseMetadata = AcmResponseMetadata.create(responseMetadata);
            return this;
        }
    }
}
