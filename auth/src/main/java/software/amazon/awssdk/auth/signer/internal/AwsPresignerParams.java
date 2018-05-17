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

public class AwsPresignerParams extends AwsSignerParams {

    private Date expirationDate;

    private AwsPresignerParams(BuilderImpl builder) {
        super(builder);
        this.expirationDate = builder.expirationDate;
    }

    public static BuilderImpl builder() {
        return new BuilderImpl();
    }

    public Date expirationDate() {
        return expirationDate;
    }

    public static class BuilderImpl extends AwsSignerParams.BuilderImpl {
        private Date expirationDate;

        public BuilderImpl expirationDate(Date expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public BuilderImpl awsCredentials(AwsCredentials awsCredentials) {
            super.awsCredentials(awsCredentials);
            return this;
        }

        public BuilderImpl signingName(String signingName) {
            super.signingName(signingName);
            return this;
        }

        public BuilderImpl region(Region region) {
            super.region(region);
            return this;
        }

        public BuilderImpl timeOffset(Integer timeOffset) {
            super.timeOffset(timeOffset);
            return this;
        }

        public BuilderImpl doubleUrlEncode(Boolean doubleUrlEncode) {
            super.doubleUrlEncode(doubleUrlEncode);
            return this;
        }

        public BuilderImpl signingDateOverride(Date signingDateOverride) {
            super.signingDateOverride(signingDateOverride);
            return this;
        }

        public AwsPresignerParams build() {
            return new AwsPresignerParams(this);
        }
    }
}