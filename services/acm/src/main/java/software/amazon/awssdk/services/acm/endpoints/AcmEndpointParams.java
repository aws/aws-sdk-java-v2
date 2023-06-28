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

package software.amazon.awssdk.services.acm.endpoints;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.Region;

/**
 * The parameters object used to resolve an endpoint for the Acm service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class AcmEndpointParams {
    private final Region region;

    private final Boolean useDualStack;

    private final Boolean useFIPS;

    private final String endpoint;

    private AcmEndpointParams(BuilderImpl builder) {
        this.region = builder.region;
        this.useDualStack = builder.useDualStack;
        this.useFIPS = builder.useFIPS;
        this.endpoint = builder.endpoint;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public Region region() {
        return region;
    }

    public Boolean useDualStack() {
        return useDualStack;
    }

    public Boolean useFips() {
        return useFIPS;
    }

    public String endpoint() {
        return endpoint;
    }

    public interface Builder {
        Builder region(Region region);

        Builder useDualStack(Boolean useDualStack);

        Builder useFips(Boolean useFIPS);

        Builder endpoint(String endpoint);

        AcmEndpointParams build();
    }

    private static class BuilderImpl implements Builder {
        private Region region;

        private Boolean useDualStack = false;

        private Boolean useFIPS = false;

        private String endpoint;

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder useDualStack(Boolean useDualStack) {
            this.useDualStack = useDualStack;
            if (this.useDualStack == null) {
                this.useDualStack = false;
            }
            return this;
        }

        @Override
        public Builder useFips(Boolean useFIPS) {
            this.useFIPS = useFIPS;
            if (this.useFIPS == null) {
                this.useFIPS = false;
            }
            return this;
        }

        @Override
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public AcmEndpointParams build() {
            return new AcmEndpointParams(this);
        }
    }
}
