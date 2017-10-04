/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.regions.providers;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.regions.Region;

/**
 * Interface for providing AWS region information. Implementations are free to use any strategy for
 * providing region information.
 */
@SdkInternalApi
public abstract class AwsRegionProvider {

    /**
     * @return Region name to use or null if region information is not available.
     */
    @ReviewBeforeRelease("Should this be Optional and have the same contract as credential providers?")
    public abstract Region getRegion() throws SdkClientException;

}
