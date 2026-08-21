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

package software.amazon.awssdk.benchmark.dynamodb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.benchmark.dynamodb.live.LiveBenchmarkGuard;

/**
 * Guard-only tests (no AWS). Ensures live opt-in fails closed before any resource work.
 */
public class LiveBenchmarkGuardTest {

    private String previousProp;

    @After
    public void restore() {
        if (previousProp == null) {
            System.clearProperty(DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.property());
        } else {
            System.setProperty(DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.property(), previousProp);
        }
    }

    @Test
    public void requireLiveOptIn_withoutProperty_throwsBeforeAwsWork() {
        previousProp = System.getProperty(DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.property());
        System.clearProperty(DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.property());
        // If the process environment already opts in, skip this assertion.
        if (DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.getBooleanValue().orElse(false)) {
            return;
        }
        assertFalse(LiveBenchmarkGuard.isLiveOptInEnabled());
        try {
            LiveBenchmarkGuard.requireLiveOptIn();
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(
                DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.environmentVariable()));
            assertTrue(expected.getMessage().contains(
                DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.property()));
        }
    }

    @Test
    public void requireLiveOptIn_withSystemProperty_passes() {
        previousProp = System.getProperty(DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.property());
        System.setProperty(DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.property(), "true");
        assertTrue(LiveBenchmarkGuard.isLiveOptInEnabled());
        LiveBenchmarkGuard.requireLiveOptIn();
    }
}
