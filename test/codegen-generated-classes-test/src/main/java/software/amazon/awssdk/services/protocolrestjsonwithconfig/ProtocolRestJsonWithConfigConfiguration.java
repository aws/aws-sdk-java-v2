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

package software.amazon.awssdk.services.protocolrestjsonwithconfig;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkPublicApi
public class ProtocolRestJsonWithConfigConfiguration
    implements ServiceConfiguration,
               ToCopyableBuilder<ProtocolRestJsonWithConfigConfiguration.Builder, ProtocolRestJsonWithConfigConfiguration> {
    private final Boolean dualstackEnabled;

    private ProtocolRestJsonWithConfigConfiguration(Builder b) {
        this.dualstackEnabled = b.dualstackEnabled;
    }

    public boolean dualstackEnabled() {
        if (dualstackEnabled != null) {
            return dualstackEnabled;
        }

        return false;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements CopyableBuilder<Builder, ProtocolRestJsonWithConfigConfiguration> {
        private Boolean dualstackEnabled;

        private Builder() {
        }

        private Builder(ProtocolRestJsonWithConfigConfiguration config) {
            this.dualstackEnabled = config.dualstackEnabled;
        }

        public Builder dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }


        public Boolean dualstackEnabled() {
            return dualstackEnabled;
        }

        // no-op
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            return this;
        }

        // no-op
        public Builder profileName(String profileName) {
            return this;
        }

        // no-op
        public String profileName() {
            return null;
        }

        // no-op
        public Supplier<ProfileFile> profileFileSupplier() {
            return null;
        }

        public ProtocolRestJsonWithConfigConfiguration build() {
            return new ProtocolRestJsonWithConfigConfiguration(this);
        }
    }
}
