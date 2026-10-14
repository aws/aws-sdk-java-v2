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

package software.amazon.awssdk.s3.wiretap.testarchive;

import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public enum ChecksumModeOption {

    ENABLED("enabled"),
    UNSET("unset");

    private final String label;

    ChecksumModeOption(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public GetObjectRequest.Builder apply(GetObjectRequest.Builder builder) {
        return this == ENABLED ? builder.checksumMode(ChecksumMode.ENABLED) : builder;
    }

    public HeadObjectRequest.Builder apply(HeadObjectRequest.Builder builder) {
        return this == ENABLED ? builder.checksumMode(ChecksumMode.ENABLED) : builder;
    }

    public boolean enabled() {
        return this == ENABLED;
    }
}
