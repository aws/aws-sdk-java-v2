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

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public enum CredentialSourceType {
    EC2_INSTANCE_METADATA,
    ECS_CONTAINER,
    ENVIRONMENT;

    public static CredentialSourceType parse(String value) {
        if (value.equalsIgnoreCase("Ec2InstanceMetadata")) {
            return EC2_INSTANCE_METADATA;
        } else if (value.equalsIgnoreCase("EcsContainer")) {
            return ECS_CONTAINER;
        } else if (value.equalsIgnoreCase("Environment")) {
            return ENVIRONMENT;
        }

        throw new IllegalArgumentException(String.format("%s is not a valid credential_source", value));
    }
}
