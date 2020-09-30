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

package software.amazon.awssdk.services.s3.internal;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkInternalApi
public class ConfiguredS3SdkHttpRequest
        implements ToCopyableBuilder<ConfiguredS3SdkHttpRequest.Builder, ConfiguredS3SdkHttpRequest> {
    private final SdkHttpRequest sdkHttpRequest;
    private final Region signingRegionModification;
    private final String signingServiceModification;

    private ConfiguredS3SdkHttpRequest(Builder builder) {
        this.sdkHttpRequest = Validate.notNull(builder.sdkHttpRequest, "sdkHttpRequest");
        this.signingRegionModification = builder.signingRegionModification;
        this.signingServiceModification = builder.signingServiceModification;
    }

    public static Builder builder() {
        return new Builder();
    }

    public SdkHttpRequest sdkHttpRequest() {
        return sdkHttpRequest;
    }

    public Optional<Region> signingRegionModification() {
        return Optional.ofNullable(signingRegionModification);
    }

    public Optional<String> signingServiceModification() {
        return Optional.ofNullable(signingServiceModification);
    }

    @Override
    public Builder toBuilder() {
        return builder().sdkHttpRequest(sdkHttpRequest).signingRegionModification(signingRegionModification);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfiguredS3SdkHttpRequest that = (ConfiguredS3SdkHttpRequest) o;

        if (!sdkHttpRequest.equals(that.sdkHttpRequest)) {
            return false;
        }
        if (signingRegionModification != null ? !signingRegionModification.equals(that.signingRegionModification) :
            that.signingRegionModification != null) {
            return false;
        }
        return signingServiceModification != null ? signingServiceModification.equals(that.signingServiceModification) :
               that.signingServiceModification == null;
    }

    @Override
    public int hashCode() {
        int result = sdkHttpRequest.hashCode();
        result = 31 * result + (signingRegionModification != null ? signingRegionModification.hashCode() : 0);
        result = 31 * result + (signingServiceModification != null ? signingServiceModification.hashCode() : 0);
        return result;
    }

    public static class Builder implements CopyableBuilder<Builder, ConfiguredS3SdkHttpRequest> {
        private String signingServiceModification;
        private SdkHttpRequest sdkHttpRequest;
        private Region signingRegionModification;

        private Builder() {
        }

        public Builder sdkHttpRequest(SdkHttpRequest sdkHttpRequest) {
            this.sdkHttpRequest = sdkHttpRequest;
            return this;
        }

        public Builder signingRegionModification(Region signingRegionModification) {
            this.signingRegionModification = signingRegionModification;
            return this;
        }

        public Builder signingServiceModification(String signingServiceModification) {
            this.signingServiceModification = signingServiceModification;
            return this;
        }

        @Override
        public ConfiguredS3SdkHttpRequest build() {
            return new ConfiguredS3SdkHttpRequest(this);
        }
    }

}
