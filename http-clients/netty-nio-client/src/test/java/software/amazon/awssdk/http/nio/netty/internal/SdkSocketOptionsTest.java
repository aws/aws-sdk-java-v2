/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.junit.Assert.assertTrue;

import io.netty.channel.ChannelOption;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class SdkSocketOptionsTest {

    @Test
    public void defaultSdkSocketOptionPresent() {
        SdkSocketOptions socketOptions = new SdkSocketOptions();
        assertEquals(1, socketOptions.getSocketOptions().size());
        Map.Entry<ChannelOption, Boolean> entry = new SimpleImmutableEntry<>(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        Set<Map.Entry<ChannelOption, Boolean>> expectedOptions = new HashSet<>();
        expectedOptions.add(entry);

        assertEquals(expectedOptions, socketOptions.getSocketOptions());
    }

    @Test
    public void additionalSdkSocketOptionsPresent() {
        SdkSocketOptions socketOptions = new SdkSocketOptions();
        assertEquals(1, socketOptions.getSocketOptions().size());
        Map.Entry<ChannelOption, Boolean> entry = new SimpleImmutableEntry<>(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        Map.Entry<ChannelOption, Integer> additionalEntry = new SimpleImmutableEntry<>(ChannelOption.SO_LINGER, 0);
        socketOptions.addOption(ChannelOption.SO_LINGER, 0);

        assertTrue(socketOptions.getSocketOptions().contains(entry));
        assertTrue(socketOptions.getSocketOptions().contains(additionalEntry));
    }
}
