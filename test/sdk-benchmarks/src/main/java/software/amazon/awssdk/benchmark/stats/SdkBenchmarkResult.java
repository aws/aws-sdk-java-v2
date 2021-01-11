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

package software.amazon.awssdk.benchmark.stats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SDK wrapper of benchmark result, created for easy serialization/deserialization.
 */
public class SdkBenchmarkResult {

    private String id;

    private SdkBenchmarkParams params;

    private SdkBenchmarkStatistics statistics;

    @JsonCreator
    public SdkBenchmarkResult(@JsonProperty("id") String benchmarkId,
                              @JsonProperty("params") SdkBenchmarkParams params,
                              @JsonProperty("statistics") SdkBenchmarkStatistics statistics) {
        this.id = benchmarkId;
        this.statistics = statistics;
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SdkBenchmarkStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(SdkBenchmarkStatistics statistics) {
        this.statistics = statistics;
    }

    public SdkBenchmarkParams getParams() {
        return params;
    }

    public void setParams(SdkBenchmarkParams params) {
        this.params = params;
    }
}
