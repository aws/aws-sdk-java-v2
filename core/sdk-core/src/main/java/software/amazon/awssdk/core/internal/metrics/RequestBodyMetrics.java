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

package software.amazon.awssdk.core.internal.metrics;

import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Container for request body metrics used to calculate WRITE_THROUGHPUT.
 */
@SdkInternalApi
public final class RequestBodyMetrics {
    private final AtomicLong bytesWritten;
    private final AtomicLong firstByteWrittenNanoTime;
    private final AtomicLong lastByteWrittenNanoTime;

    public RequestBodyMetrics() {
        this.bytesWritten = new AtomicLong(0);
        this.firstByteWrittenNanoTime = new AtomicLong(0);
        this.lastByteWrittenNanoTime = new AtomicLong(0);
    }

    public AtomicLong bytesWritten() {
        return bytesWritten;
    }

    public AtomicLong firstByteWrittenNanoTime() {
        return firstByteWrittenNanoTime;
    }

    public AtomicLong lastByteWrittenNanoTime() {
        return lastByteWrittenNanoTime;
    }
}
