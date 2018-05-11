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

package software.amazon.awssdk.regions.providers;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;

/**
 * Interface for providing AWS region information. Implementations are free to use any strategy for
 * providing region information.
 */
@SdkProtectedApi
@FunctionalInterface
public interface AwsRegionProvider {
    /**
     * @return Region name to use or null if region information is not available.
     */
    @ReviewBeforeRelease("Should this throw exceptions so that its contract matches that of the credential providers? This is " +
                         "currently a protected API used in STS, so we should decide before GA.")
    Region getRegion() throws SdkClientException;

}
