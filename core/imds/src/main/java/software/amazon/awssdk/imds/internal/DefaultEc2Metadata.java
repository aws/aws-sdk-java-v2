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

package software.amazon.awssdk.imds.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.imds.Ec2Metadata;

/**
 * An Implementation of the Ec2Metadata Interface.
 */
@SdkInternalApi
public class DefaultEc2Metadata implements Ec2Metadata {


    /**
     * Gets the specified instance metadata value by the given path.
     * @param path  Input path
     * @return Instance metadata value
     */
    @Override
    public String get(String path) {
        return "IMDS";
    }

}
