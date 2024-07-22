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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

public class ChannelDiagnosticsTest {
    @Test
    public void stopIdleTimer_noPreviousCallToStartIdleTimer_noDurationCalculated() {
        ChannelDiagnostics cd = new ChannelDiagnostics(new EmbeddedChannel());
        cd.stopIdleTimer();
        assertThat(cd.lastIdleDuration()).isNull();
    }

    @Test
    public void stopIdleTimer_previousCallToStartIdleTimer_durationCalculated() {
        ChannelDiagnostics cd = new ChannelDiagnostics(new EmbeddedChannel());
        cd.startIdleTimer();
        cd.stopIdleTimer();
        assertThat(cd.lastIdleDuration().toNanos()).isPositive();
    }

    @Test
    public void incrementResponseCount_reflectsCorrectValue() {
        ChannelDiagnostics cd = new ChannelDiagnostics(new EmbeddedChannel());
        int count = 42;
        for (int i = 0; i < count; ++i) {
            cd.incrementResponseCount();
        }
        assertThat(cd.responseCount()).isEqualTo(count);
    }
}
