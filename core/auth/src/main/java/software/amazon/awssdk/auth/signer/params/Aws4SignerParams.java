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

package software.amazon.awssdk.auth.signer.params;

import java.time.Clock;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

/**
 * Parameters that are used during signing.
 *
 * Required parameters vary based on signer implementations. Signer implementations might only use a
 * subset of params in this class.
 */
@SdkPublicApi
public class Aws4SignerParams {
    private final Boolean doubleUrlEncode;
    private final AwsCredentials awsCredentials;
    private final String signingName;
    private final Region signingRegion;
    private final Integer timeOffset;
    private final Clock signingClockOverride;

    Aws4SignerParams(BuilderImpl<?> builder) {
        this.doubleUrlEncode = Validate.paramNotNull(builder.doubleUrlEncode, "Double Url encode");
        this.awsCredentials = Validate.paramNotNull(builder.awsCredentials, "Credentials");
        this.signingName = Validate.paramNotNull(builder.signingName, "service signing name");
        this.signingRegion = Validate.paramNotNull(builder.signingRegion, "signing region");
        this.timeOffset = builder.timeOffset;
        this.signingClockOverride = builder.signingClockOverride;
    }

    public static Builder builder() {
        return new BuilderImpl<>();
    }

    public Boolean doubleUrlEncode() {
        return doubleUrlEncode;
    }

    public AwsCredentials awsCredentials() {
        return awsCredentials;
    }

    public String signingName() {
        return signingName;
    }

    public Region signingRegion() {
        return signingRegion;
    }

    public Optional<Integer> timeOffset() {
        return Optional.ofNullable(timeOffset);
    }

    public Optional<Clock> signingClockOverride() {
        return Optional.ofNullable(signingClockOverride);
    }

    public interface Builder<B extends Builder> {

        /**
         * Set this value to double url-encode the resource path when constructing the
         * canonical request.
         *
         * By default, all services except S3 enable double url-encoding.
         *
         * @param doubleUrlEncode Set true to enable double url encoding. Otherwise false.
         */
        B doubleUrlEncode(Boolean doubleUrlEncode);

        /**
         * Sets the aws credentials to use for computing the signature.
         *
         * @param awsCredentials Aws Credentials to use for computing the signature.
         */
        B awsCredentials(AwsCredentials awsCredentials);

        /**
         * The name of the AWS service to be used for computing the signature.
         *
         * @param signingName Name of the AWS service to be used for computing the signature.
         */
        B signingName(String signingName);

        /**
         * The AWS region to be used for computing the signature.
         *
         * @param signingRegion AWS region to be used for computing the signature.
         */
        B signingRegion(Region signingRegion);

        /**
         * The time offset (for clock skew correction) to use when computing the signing date for the request.
         *
         * @param timeOffset The time offset (for clock skew correction) to use when computing the signing date for the request.
         */
        B timeOffset(Integer timeOffset);

        /**
         * The clock to use for overriding the signing time when computing signature for a request.
         *
         * By default, current time of the system is used for signing. This parameter can be used to set custom signing time.
         * Useful option for testing.
         *
         * @param signingClockOverride The clock to use for overriding the signing time when computing signature for a request.
         */
        B signingClockOverride(Clock signingClockOverride);

        Aws4SignerParams build();
    }

    protected static class BuilderImpl<B extends Builder> implements Builder<B> {
        private static final Boolean DEFAULT_DOUBLE_URL_ENCODE = Boolean.TRUE;

        private Boolean doubleUrlEncode = DEFAULT_DOUBLE_URL_ENCODE;
        private AwsCredentials awsCredentials;
        private String signingName;
        private Region signingRegion;
        private Integer timeOffset;
        private Clock signingClockOverride;

        protected BuilderImpl() {

        }

        @Override
        public B doubleUrlEncode(Boolean doubleUrlEncode) {
            this.doubleUrlEncode = doubleUrlEncode;
            return (B) this;
        }

        public void setDoubleUrlEncode(Boolean doubleUrlEncode) {
            doubleUrlEncode(doubleUrlEncode);
        }

        @Override
        public B awsCredentials(AwsCredentials awsCredentials) {
            this.awsCredentials = awsCredentials;
            return (B) this;
        }

        public void setAwsCredentials(AwsCredentials awsCredentials) {
            awsCredentials(awsCredentials);
        }

        @Override
        public B signingName(String signingName) {
            this.signingName = signingName;
            return (B) this;
        }

        public void setSigningName(String signingName) {
            signingName(signingName);
        }

        @Override
        public B signingRegion(Region signingRegion) {
            this.signingRegion = signingRegion;
            return (B) this;
        }

        public void setSigningRegion(Region signingRegion) {
            signingRegion(signingRegion);
        }

        @Override
        public B timeOffset(Integer timeOffset) {
            this.timeOffset = timeOffset;
            return (B) this;
        }

        public void setTimeOffset(Integer timeOffset) {
            timeOffset(timeOffset);
        }

        @Override
        public B signingClockOverride(Clock signingClockOverride) {
            this.signingClockOverride = signingClockOverride;
            return (B) this;
        }

        public void setSigningClockOverride(Clock signingClockOverride) {
            signingClockOverride(signingClockOverride);
        }

        @Override
        public Aws4SignerParams build() {
            return new Aws4SignerParams(this);
        }
    }
}