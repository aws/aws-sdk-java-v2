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

package software.amazon.awssdk.services.s3.internal.multipart;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.multipart.ParallelConfiguration;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal utility class to resolve {@link MultipartConfiguration}.
 */
@SdkInternalApi
public final class MultipartConfigurationResolver {

    private static final long DEFAULT_MIN_PART_SIZE = 8L * 1024 * 1024;

    // Using 50 as the default since maxConcurrency for http client is also 50
    private static final int DEFAULT_MAX_IN_FLIGHT_PARTS = 50;

    private final long minimalPartSizeInBytes;
    private final long apiCallBufferSize;
    private final long thresholdInBytes;
    private final int maxInFlightParts;

    public MultipartConfigurationResolver(MultipartConfiguration multipartConfiguration) {
        Validate.notNull(multipartConfiguration, "multipartConfiguration");
        this.minimalPartSizeInBytes = Validate.getOrDefault(multipartConfiguration.minimumPartSizeInBytes(),
                                                            () -> DEFAULT_MIN_PART_SIZE);
        this.apiCallBufferSize = Validate.getOrDefault(multipartConfiguration.apiCallBufferSizeInBytes(),
                                                       () -> minimalPartSizeInBytes * 4);
        this.thresholdInBytes = Validate.getOrDefault(multipartConfiguration.thresholdInBytes(), () -> minimalPartSizeInBytes);
        ParallelConfiguration parallelConfiguration = multipartConfiguration.parallelConfiguration();
        if (parallelConfiguration == null) {
            this.maxInFlightParts = DEFAULT_MAX_IN_FLIGHT_PARTS;
        } else {
            this.maxInFlightParts = Validate.getOrDefault(multipartConfiguration.parallelConfiguration().maxInFlightParts(),
                                                          () -> DEFAULT_MAX_IN_FLIGHT_PARTS);
        }
    }

    public long minimalPartSizeInBytes() {
        return minimalPartSizeInBytes;
    }

    public long thresholdInBytes() {
        return thresholdInBytes;
    }

    public long apiCallBufferSize() {
        return apiCallBufferSize;
    }

    public int maxInFlightParts() {
        return maxInFlightParts;
    }
}
