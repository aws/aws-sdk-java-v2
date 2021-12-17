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

package software.amazon.awssdk.core.internal.retry;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.DateUtils;

public class ClockSkewAdjusterTest {
    private ClockSkewAdjuster adjuster = new ClockSkewAdjuster();

    @Test
    public void adjustmentTranslatesCorrectly() {
        assertThat(adjuster.getAdjustmentInSeconds(responseWithDateOffset(1, HOURS))).isCloseTo(-1 * 60 * 60, within(5));
        assertThat(adjuster.getAdjustmentInSeconds(responseWithDateOffset(-14, MINUTES))).isCloseTo(14 * 60, within(5));
    }

    @Test
    public void farFutureDateTranslatesToZero() {
        assertThat(adjuster.getAdjustmentInSeconds(responseWithDate("Fri, 31 Dec 9999 23:59:59 GMT"))).isEqualTo(0);
    }

    @Test
    public void badDateTranslatesToZero() {
        assertThat(adjuster.getAdjustmentInSeconds(responseWithDate("X"))).isEqualTo(0);
    }

    private SdkHttpFullResponse responseWithDateOffset(int value, ChronoUnit unit) {
        return SdkHttpFullResponse.builder()
                                  .putHeader("Date", DateUtils.formatRfc1123Date(Instant.now().plus(value, unit)))
                                  .build();
    }

    private SdkHttpFullResponse responseWithDate(String date) {
        return SdkHttpFullResponse.builder()
                                  .putHeader("Date", date)
                                  .build();
    }
}