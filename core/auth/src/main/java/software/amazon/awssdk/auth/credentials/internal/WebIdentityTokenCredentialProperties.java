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

package software.amazon.awssdk.auth.credentials.internal;

import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A container for credential properties.
 */
@SdkProtectedApi
public class WebIdentityTokenCredentialProperties {

    private final String roleArn;
    private final String roleSessionName;
    private final Path webIdentityTokenFile;

    private WebIdentityTokenCredentialProperties(Builder builder) {
        this.roleArn = builder.roleArn;
        this.roleSessionName = builder.roleSessionName;
        this.webIdentityTokenFile = builder.webIdentityTokenFile;
    }

    public String roleArn() {
        return roleArn;
    }

    public String roleSessionName() {
        return roleSessionName;
    }

    public Path webIdentityTokenFile() {
        return webIdentityTokenFile;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String roleArn;
        private String roleSessionName;
        private Path webIdentityTokenFile;

        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        public Builder roleSessionName(String roleSessionName) {
            this.roleSessionName = roleSessionName;
            return this;
        }

        public Builder webIdentityTokenFile(Path webIdentityTokenFile) {
            this.webIdentityTokenFile = webIdentityTokenFile;
            return this;
        }

        public WebIdentityTokenCredentialProperties build() {
            return new WebIdentityTokenCredentialProperties(this);
        }
    }
}
