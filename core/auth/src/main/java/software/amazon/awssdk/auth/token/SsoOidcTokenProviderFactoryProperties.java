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

package software.amazon.awssdk.auth.token;

import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class SsoOidcTokenProviderFactoryProperties {
    private String startUrl;

    private SsoOidcTokenProviderFactoryProperties(BuilderImpl builder) {
        this.startUrl = builder.startUrl;
    }

    public String startUrl() {
        return startUrl;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        Builder startUrl(String startUrl);

        SsoOidcTokenProviderFactoryProperties build();
    }

    private static class BuilderImpl implements Builder {
        private String startUrl;

        @Override
        public Builder startUrl(String startUrl) {
            this.startUrl = startUrl;
            return this;
        }

        @Override
        public SsoOidcTokenProviderFactoryProperties build() {
            return new SsoOidcTokenProviderFactoryProperties(this);
        }
    }
}
