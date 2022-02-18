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

package software.amazon.awssdk.auth.token.credentials;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public class SdkTokenProviderFactoryProperties {
    private final String startUrl;
    private final String region;

    private SdkTokenProviderFactoryProperties(BuilderImpl builder) {
        Validate.paramNotNull(builder.startUrl, "startUrl");
        Validate.paramNotNull(builder.region, "region");

        this.startUrl = builder.startUrl;
        this.region = builder.region;
    }

    public String startUrl() {
        return startUrl;
    }

    public String region() {
        return region;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        Builder startUrl(String startUrl);

        Builder region(String region);

        SdkTokenProviderFactoryProperties build();
    }

    private static class BuilderImpl implements Builder {
        private String startUrl;
        private String region;

        @Override
        public Builder startUrl(String startUrl) {
            this.startUrl = startUrl;
            return this;
        }

        @Override
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        @Override
        public SdkTokenProviderFactoryProperties build() {
            return new SdkTokenProviderFactoryProperties(this);
        }
    }
}
