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

    private static final boolean DEFAULT_DOUBLE_URL_ENCODE = true;

    private boolean doubleUrlEncode = DEFAULT_DOUBLE_URL_ENCODE;

    private AwsCredentials awsCredentials;

    private String signingName;

    private Region region;

    private Integer timeOffset;

    private Date signingDateOverride;

    public Date getSigningDateOverride() {
        return signingDateOverride;
    }

    public void setSigningDateOverride(Date signingDateOverride) {
        this.signingDateOverride = signingDateOverride;
    }

    public boolean isDoubleUrlEncode() {
        return doubleUrlEncode;
    }

    public void setDoubleUrlEncode(boolean doubleUrlEncode) {
        this.doubleUrlEncode = doubleUrlEncode;
    }


    public AwsCredentials getAwsCredentials() {
        return awsCredentials;
    }

    public void setAwsCredentials(AwsCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
    }

    public String getSigningName() {
        return signingName;
    }

    public void setSigningName(String signingName) {
        this.signingName = signingName;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Integer getTimeOffset() {
        return timeOffset;
    }

    public void setTimeOffset(Integer timeOffset) {
        this.timeOffset = timeOffset;
    }
}