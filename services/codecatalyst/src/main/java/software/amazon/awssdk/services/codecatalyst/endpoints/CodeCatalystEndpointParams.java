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

package software.amazon.awssdk.services.codecatalyst.endpoints;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.Region;

/**
 * The parameters object used to resolve an endpoint for the CodeCatalyst service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class CodeCatalystEndpointParams {
    private final Boolean useFIPS;

    private final Region region;

    private final String endpoint;

    private CodeCatalystEndpointParams(BuilderImpl builder) {
        this.useFIPS = builder.useFIPS;
        this.region = builder.region;
        this.endpoint = builder.endpoint;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public Boolean useFips() {
        return useFIPS;
    }

    public Region region() {
        return region;
    }

    public String endpoint() {
        return endpoint;
    }

    public interface Builder {
        Builder useFips(Boolean useFIPS);

        Builder region(Region region);

        Builder endpoint(String endpoint);

        CodeCatalystEndpointParams build();
    }

    private static class BuilderImpl implements Builder {
        private Boolean useFIPS = false;

        private Region region;

        private String endpoint;

        @Override
        public Builder useFips(Boolean useFIPS) {
            this.useFIPS = useFIPS;
            if (this.useFIPS == null) {
                this.useFIPS = false;
            }
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public CodeCatalystEndpointParams build() {
            return new CodeCatalystEndpointParams(this);
        }
    }
}
