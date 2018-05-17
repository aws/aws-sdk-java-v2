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

package software.amazon.awssdk.auth.signer.internal;

import java.util.Date;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

/**
 * Parameters that are used during signing.
 *
 * Required parameters vary based on signer implementations. Signer implementations might only use a
 * subset of params in this class.
 */
public class AwsSignerParams {

    private final Boolean doubleUrlEncode;
    private final AwsCredentials awsCredentials;
    private final String signingName;
    private final Region region;
    private final Integer timeOffset;
    private final Date signingDateOverride;

    protected AwsSignerParams(BuilderImpl builder) {
        this.doubleUrlEncode = builder.doubleUrlEncode;
        this.awsCredentials = builder.awsCredentials;
        this.signingName = builder.signingName;
        this.region = builder.region;
        this.timeOffset = builder.timeOffset;
        this.signingDateOverride = builder.signingDateOverride;
    }

    public static BuilderImpl builder() {
        return new BuilderImpl();
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

    public Region region() {
        return region;
    }

    public Integer timeOffset() {
        return timeOffset;
    }

    public Date getSigningDateOverride() {
        return signingDateOverride;
    }

    public static class BuilderImpl {
        private static final Boolean DEFAULT_DOUBLE_URL_ENCODE = Boolean.TRUE;

        private Boolean doubleUrlEncode = DEFAULT_DOUBLE_URL_ENCODE;
        private AwsCredentials awsCredentials;
        private String signingName;
        private Region region;
        private Integer timeOffset;
        private Date signingDateOverride;

        public BuilderImpl awsCredentials(AwsCredentials awsCredentials) {
            this.awsCredentials = awsCredentials;
            return this;
        }

        public BuilderImpl signingName(String signingName) {
            this.signingName = signingName;
            return this;
        }

        public BuilderImpl region(Region region) {
            this.region = region;
            return this;
        }

        public BuilderImpl timeOffset(Integer timeOffset) {
            this.timeOffset = timeOffset;
            return this;
        }

        public BuilderImpl doubleUrlEncode(Boolean doubleUrlEncode) {
            this.doubleUrlEncode = doubleUrlEncode;
            return this;
        }

        public BuilderImpl signingDateOverride(Date signingDateOverride) {
            this.signingDateOverride = signingDateOverride;
            return this;
        }

        public AwsSignerParams build() {
            return new AwsSignerParams(this);
        }
    }
}