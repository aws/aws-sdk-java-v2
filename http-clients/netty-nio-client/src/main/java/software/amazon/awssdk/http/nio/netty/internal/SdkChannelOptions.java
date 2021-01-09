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

import io.netty.channel.ChannelOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class SdkChannelOptions {

    private Map<ChannelOption, Object> options;

    public SdkChannelOptions() {
        options = new HashMap<>();
        options.put(ChannelOption.TCP_NODELAY, Boolean.TRUE);
    }

    public <T> SdkChannelOptions putOption(ChannelOption<T> channelOption, T channelOptionValue) {
        channelOption.validate(channelOptionValue);
        options.put(channelOption, channelOptionValue);
        return this;
    }

    public Map<ChannelOption, Object> channelOptions() {
        return Collections.unmodifiableMap(options);
    }
}
