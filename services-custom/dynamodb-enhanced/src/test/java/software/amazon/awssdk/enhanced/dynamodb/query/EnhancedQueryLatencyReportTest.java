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

package software.amazon.awssdk.enhanced.dynamodb.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryLatencyReport;

/**
 * Unit tests for {@link EnhancedQueryLatencyReport}.
 */
public class EnhancedQueryLatencyReportTest {

    @Test
    public void gettersReturnValuesPassedToConstructor() {
        EnhancedQueryLatencyReport report = new EnhancedQueryLatencyReport(1L, 2L, 3L, 4L);

        assertThat(report.baseQueryMs()).isEqualTo(1L);
        assertThat(report.joinedLookupsMs()).isEqualTo(2L);
        assertThat(report.inMemoryProcessingMs()).isEqualTo(3L);
        assertThat(report.totalMs()).isEqualTo(4L);
        assertThat(report.baseQueryRequestCount()).isEqualTo(0L);
        assertThat(report.baseScanRequestCount()).isEqualTo(0L);
        assertThat(report.joinedQueryRequestCount()).isEqualTo(0L);
        assertThat(report.joinedScanRequestCount()).isEqualTo(0L);
        assertThat(report.totalDynamoDbRequestCount()).isEqualTo(0L);
        assertThat(report.baseQueryRcuConsumed()).isEqualTo(0.0d);
        assertThat(report.baseScanRcuConsumed()).isEqualTo(0.0d);
        assertThat(report.joinedQueryRcuConsumed()).isEqualTo(0.0d);
        assertThat(report.joinedScanRcuConsumed()).isEqualTo(0.0d);
        assertThat(report.totalRcuConsumed()).isEqualTo(0.0d);
    }

    @Test
    public void requestCountGetters_sumToTotal() {
        EnhancedQueryLatencyReport report = new EnhancedQueryLatencyReport(0L, 0L, 0L, 10L, 2L, 1L, 5L, 3L);
        assertThat(report.baseQueryRequestCount()).isEqualTo(2L);
        assertThat(report.baseScanRequestCount()).isEqualTo(1L);
        assertThat(report.joinedQueryRequestCount()).isEqualTo(5L);
        assertThat(report.joinedScanRequestCount()).isEqualTo(3L);
        assertThat(report.totalDynamoDbRequestCount()).isEqualTo(11L);
        assertThat(report.totalRcuConsumed()).isEqualTo(0.0d);
    }

    @Test
    public void rcuGetters_sumToTotal() {
        EnhancedQueryLatencyReport report =
            new EnhancedQueryLatencyReport(0L, 0L, 0L, 10L, 2L, 1L, 5L, 3L, 1.25d, 2.75d, 3.5d, 4.0d);

        assertThat(report.baseQueryRcuConsumed()).isEqualTo(1.25d);
        assertThat(report.baseScanRcuConsumed()).isEqualTo(2.75d);
        assertThat(report.joinedQueryRcuConsumed()).isEqualTo(3.5d);
        assertThat(report.joinedScanRcuConsumed()).isEqualTo(4.0d);
        assertThat(report.totalRcuConsumed()).isEqualTo(11.5d);
    }
}
