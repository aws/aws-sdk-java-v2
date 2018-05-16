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
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

/**
 * Parameters that are used during signing.
 *
 * Required parameters vary based on signer implementations. Signer implementations might only use a
 * subset of params in this class.
 */
public class Aws4SignerParams {
    private final Boolean doubleUrlEncode;
    private final AwsCredentials awsCredentials;
    private final String signingName;
    private final Region signingRegion;
    private final Integer timeOffset;
    private final Clock signingClockOverride;

    protected Aws4SignerParams(Builder builder) {
        this.doubleUrlEncode = Validate.paramNotNull(builder.doubleUrlEncode, "Double Url encode");
        this.awsCredentials = Validate.paramNotNull(builder.awsCredentials, "Credentials");
        this.signingName = Validate.paramNotNull(builder.signingName, "service signing name");
        this.signingRegion = Validate.paramNotNull(builder.signingRegion, "signing region");
        this.timeOffset = builder.timeOffset;
        this.signingClockOverride = builder.signingClockOverride;
    }

    public static Builder builder() {
        return new Builder();
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

    public Integer timeOffset() {
        return timeOffset;
    }

    public Clock signingClockOverride() {
        return signingClockOverride;
    }

    public static class Builder<T extends Builder> {
        private static final Boolean DEFAULT_DOUBLE_URL_ENCODE = Boolean.TRUE;

        private Boolean doubleUrlEncode = DEFAULT_DOUBLE_URL_ENCODE;
        private AwsCredentials awsCredentials;
        private String signingName;
        private Region signingRegion;
        private Integer timeOffset;
        private Clock signingClockOverride;

        /**
         * Set this value to double url-encode the resource path when constructing the
         * canonical request.
         *
         * By default, all services except S3 enable double url-encoding.
         *
         * @param doubleUrlEncode Set true to enable double url encoding. Otherwise false.
         */
        public T doubleUrlEncode(Boolean doubleUrlEncode) {
            this.doubleUrlEncode = doubleUrlEncode;
            return (T) this;
        }

        /**
         * Sets the aws credentials to use for computing the signature.
         *
         * @param awsCredentials Aws Credentials to use for computing the signature.
         */
        public T awsCredentials(AwsCredentials awsCredentials) {
            this.awsCredentials = awsCredentials;
            return (T) this;
        }

        /**
         * The name of the AWS service to be used for computing the signature.
         *
         * @param signingName Name of the AWS service to be used for computing the signature.
         */
        public T signingName(String signingName) {
            this.signingName = signingName;
            return (T) this;
        }

        /**
         * The AWS region to be used for computing the signature.
         *
         * @param signingRegion AWS region to be used for computing the signature.
         */
        public T signingRegion(Region signingRegion) {
            this.signingRegion = signingRegion;
            return (T) this;
        }

        /**
         * The time offset (for clock skew correction) to use when computing the signing date for the request.
         *
         * @param timeOffset The time offset (for clock skew correction) to use when computing the signing date for the request.
         */
        public T timeOffset(Integer timeOffset) {
            this.timeOffset = timeOffset;
            return (T) this;
        }

        /**
         * The clock to use for overriding the signing time when computing signature for a request.
         *
         * By default, current time of the system is used for signing. This parameter can be used to set custom signing time.
         * Useful option for testing.
         *
         * @param signingClockOverride The clock to use for overriding the signing time when computing signature for a request.
         */
        public T signingClockOverride(Clock signingClockOverride) {
            this.signingClockOverride = signingClockOverride;
            return (T) this;
        }

        public Aws4SignerParams build() {
            return new Aws4SignerParams(this);
        }
    }
}