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

import static org.junit.Assert.assertEquals;

import io.netty.channel.ChannelOption;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class SdkChannelOptionsTest {

    @Test
    public void defaultSdkSocketOptionPresent() {
        SdkChannelOptions channelOptions = new SdkChannelOptions();

        Map<ChannelOption, Object> expectedOptions = new HashMap<>();
        expectedOptions.put(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        assertEquals(expectedOptions, channelOptions.channelOptions());
    }

    @Test
    public void additionalSdkSocketOptionsPresent() {
        SdkChannelOptions channelOptions = new SdkChannelOptions();
        channelOptions.putOption(ChannelOption.SO_LINGER, 0);

        Map<ChannelOption, Object> expectedOptions = new HashMap<>();
        expectedOptions.put(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        expectedOptions.put(ChannelOption.SO_LINGER, 0);

        assertEquals(expectedOptions, channelOptions.channelOptions());
    }
}
