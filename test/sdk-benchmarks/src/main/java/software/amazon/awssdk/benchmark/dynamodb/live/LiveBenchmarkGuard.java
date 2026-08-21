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

package software.amazon.awssdk.benchmark.dynamodb.live;

import software.amazon.awssdk.benchmark.dynamodb.DynamoDbBenchmarkSystemSetting;

/**
 * Hard safety gate for Tier D live DynamoDB benchmarks.
 *
 * <p>Must be invoked before credential resolution, client construction, or any AWS interaction.
 */
public final class LiveBenchmarkGuard {

    private LiveBenchmarkGuard() {
    }

    /**
     * @throws IllegalStateException if live opt-in is not present
     */
    public static void requireLiveOptIn() {
        if (!isLiveOptInEnabled()) {
            throw new IllegalStateException(
                "Live DynamoDB benchmarks are disabled. Refusing to resolve credentials, build "
                + "clients, or create AWS resources. Set "
                + DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.environmentVariable()
                + "=true or -D" + DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.property()
                + "=true to opt in explicitly.");
        }
    }

    public static boolean isLiveOptInEnabled() {
        return DynamoDbBenchmarkSystemSetting.LIVE_OPT_IN.getBooleanValue().orElse(false);
    }
}
