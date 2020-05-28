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

package software.amazon.awssdk.regions.providers;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.profiles.ProfileFile;

/**
 * AWS Region provider that looks for the region in this order:
 * <ol>
 *   <li>Check the 'aws.region' system property for the region.</li>
 *   <li>Check the 'AWS_REGION' environment variable for the region.</li>
 *   <li>Check the {user.home}/.aws/credentials and {user.home}/.aws/config files for the region.</li>
 *   <li>If running in EC2, check the EC2 metadata service for the region.</li>
 * </ol>
 */
@SdkProtectedApi
public final class DefaultAwsRegionProviderChain extends AwsRegionProviderChain {
    public DefaultAwsRegionProviderChain() {
        super(new SystemSettingsRegionProvider(),
              new AwsProfileRegionProvider(),
              new InstanceProfileRegionProvider());
    }

    private DefaultAwsRegionProviderChain(Builder builder) {
        super(new SystemSettingsRegionProvider(),
              new AwsProfileRegionProvider(builder.profileFile, builder.profileName),
              new InstanceProfileRegionProvider());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        private Builder() {
        }

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public DefaultAwsRegionProviderChain build() {
            return new DefaultAwsRegionProviderChain(this);
        }
    }
}
