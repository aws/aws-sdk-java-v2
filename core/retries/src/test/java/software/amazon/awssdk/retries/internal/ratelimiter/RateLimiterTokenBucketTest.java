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

package software.amazon.awssdk.retries.internal.ratelimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RateLimiterTokenBucketTest {
    private static MutableClock clock = null;
    private static RateLimiterTokenBucket tokenBucket = null;
    private static final double EPSILON = 0.0001;

    @BeforeAll
    static void setup() {
        clock = new MutableClock();
        tokenBucket = new RateLimiterTokenBucket(clock);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCase(TestCase testCase) {
        clock.setCurrent(testCase.givenTimestamp);
        RateLimiterUpdateResponse res;
        tokenBucket.tryAcquire();
        if (testCase.throttleResponse) {
            res = tokenBucket.updateRateAfterThrottling();
        } else {
            res = tokenBucket.updateRateAfterSuccess();
        }
        double measuredTxRate = res.measuredTxRate();
        assertThat(measuredTxRate).isCloseTo(testCase.expectMeasuredTxRate, within(EPSILON));
        double fillRate = res.fillRate();
        assertThat(fillRate).isCloseTo(testCase.expectFillRate, within(EPSILON));
    }


    public static Collection<TestCase> parameters() {
        return Arrays.asList(
            new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(0.2)
                .expectMeasuredTxRate(0.000000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(0.4)
                .expectMeasuredTxRate(0.000000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(0.6)
                .expectMeasuredTxRate(4.800000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(0.8)
                .expectMeasuredTxRate(4.800000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.0)
                .expectMeasuredTxRate(4.160000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.2)
                .expectMeasuredTxRate(4.160000)
                .expectFillRate(0.691200)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.4)
                .expectMeasuredTxRate(4.160000)
                .expectFillRate(1.097600)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.6)
                .expectMeasuredTxRate(5.632000)
                .expectFillRate(1.638400)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.8)
                .expectMeasuredTxRate(5.632000)
                .expectFillRate(2.332800)
            , new TestCase()
                .givenThrottleResponse()
                .givenTimestamp(2.0)
                .expectMeasuredTxRate(4.326400)
                .expectFillRate(3.028480)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(2.2)
                .expectMeasuredTxRate(4.326400)
                .expectFillRate(3.486639)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(2.4)
                .expectMeasuredTxRate(4.326400)
                .expectFillRate(3.821874)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(2.6)
                .expectMeasuredTxRate(5.665280)
                .expectFillRate(4.053386)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(2.8)
                .expectMeasuredTxRate(5.665280)
                .expectFillRate(4.200373)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(3.0)
                .expectMeasuredTxRate(4.333056)
                .expectFillRate(4.282037)
            , new TestCase()
                .givenThrottleResponse()
                .givenTimestamp(3.2)
                .expectMeasuredTxRate(4.333056)
                .expectFillRate(2.997426)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(3.4)
                .expectMeasuredTxRate(4.333056)
                .expectFillRate(3.452226)
        );
    }

    static class TestCase {
        private boolean throttleResponse;
        private double givenTimestamp;
        private double expectMeasuredTxRate;
        private double expectFillRate;

        TestCase givenSuccessResponse() {
            this.throttleResponse = false;
            return this;
        }

        TestCase givenThrottleResponse() {
            this.throttleResponse = true;
            return this;
        }

        TestCase givenTimestamp(double givenTimestamp) {
            this.givenTimestamp = givenTimestamp;
            return this;
        }

        TestCase expectMeasuredTxRate(double expectMeasuredTxRate) {
            this.expectMeasuredTxRate = expectMeasuredTxRate;
            return this;
        }

        TestCase expectFillRate(double expectFillRate) {
            this.expectFillRate = expectFillRate;
            return this;
        }

    }

    static class MutableClock implements RateLimiterTokenBucketStore.Clock {
        private double current;

        @Override
        public double time() {
            return current;
        }

        public void setCurrent(double current) {
            this.current = current;
        }
    }
}
