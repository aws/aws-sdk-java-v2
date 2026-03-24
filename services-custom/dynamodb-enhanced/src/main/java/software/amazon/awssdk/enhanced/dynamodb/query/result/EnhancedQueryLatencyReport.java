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

package software.amazon.awssdk.enhanced.dynamodb.query.result;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Captures basic latency measurements for enhanced query execution.
 * <p>
 * All values are expressed in milliseconds.
 */
@SdkInternalApi
public class EnhancedQueryLatencyReport {

    private final long baseQueryMs;
    private final long joinedLookupsMs;
    private final long inMemoryProcessingMs;
    private final long totalMs;

    public EnhancedQueryLatencyReport(long baseQueryMs,
                                      long joinedLookupsMs,
                                      long inMemoryProcessingMs,
                                      long totalMs) {
        this.baseQueryMs = baseQueryMs;
        this.joinedLookupsMs = joinedLookupsMs;
        this.inMemoryProcessingMs = inMemoryProcessingMs;
        this.totalMs = totalMs;
    }

    public long baseQueryMs() {
        return baseQueryMs;
    }

    public long joinedLookupsMs() {
        return joinedLookupsMs;
    }

    public long inMemoryProcessingMs() {
        return inMemoryProcessingMs;
    }

    public long totalMs() {
        return totalMs;
    }
}
