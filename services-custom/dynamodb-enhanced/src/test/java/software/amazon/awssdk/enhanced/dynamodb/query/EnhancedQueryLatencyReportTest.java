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
    }
}
