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

package software.amazon.awssdk.protocols.imds.internal;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.protocols.imds.Ec2Metadata;

/**
 * A class which is representation of the EC2MetaData Client . The class handles all the configurable parameters
 * and also the get Method.
 */

@SdkPublicApi
public class DefaultEc2Metadata implements Ec2Metadata {

    /**
     * @param path the path to the required parameter on the IMDS server.
     * @return a string response is given back , which maybe used for further processing.
     */
    @Override
    public String get(String path)
    {
        return "IMDS";
    }
}
