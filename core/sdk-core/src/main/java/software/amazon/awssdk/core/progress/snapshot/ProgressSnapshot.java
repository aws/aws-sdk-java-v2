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

package software.amazon.awssdk.core.progress.snapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

@Immutable
@ThreadSafe
@SdkPublicApi
public interface ProgressSnapshot {
    /**
     * The total number of bytes that have been sent or received so far.
     */
    long transferredBytes();

    /**
     * Time at which the HTTP Request header is sent
     */
    Instant startTime();

    /**
     * Elapsed time since the HTTP request header was sent to the service
     */
    Duration elapsedTime();

    /**
     * If transaction size is known, estimate time remaining for transaction completion
     * This is a predictive calculation based on the rate of transfer
     * <p>
     *     Double rateOfTimeUnitsPerByte = elapsedTime() / transferredBytes();
     *     Double estimatedTimeRemaining = rateOfTimeUnitsPerByte * (totalBytes() - transferredBytes());
     * </p>
     */
    Optional<Duration> estimatedTimeRemaining();

    /**
     *  Rate of transfer
     */
    double averageBytesPer(TimeUnit timeUnit);

    /**
     * The total size of the transfer, in bytes, or {@link Optional#empty()} if total payload being transacted is unknown
     * and not set. This could happen for streaming operations.
     */
    OptionalLong totalBytes();

    /**
     * The ratio of the {@link #totalBytes()} that has been transferred so far, or {@link Optional#empty()} if unknown.
     * This method depends on the {@link #totalBytes()} being known in order to return non-empty.
     */
    OptionalDouble ratioTransferred();

    /**
     * The total number of bytes that are remaining to be transferred, or {@link Optional#empty()} if unknown. This method depends
     * on the {@link #totalBytes()} being known in order to return non-empty.
     */
    OptionalLong remainingBytes();
}
