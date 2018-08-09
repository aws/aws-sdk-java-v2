package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.ChannelOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SdkSocketOption {


    private Map<ChannelOption<?>, Object> options;

    public <T> SdkSocketOption addOption(ChannelOption<T> channelOption, T channelOptionValue) {
        channelOption.validate(channelOptionValue);
        options.put(channelOption, channelOptionValue);
        return this;
    }

    public SdkSocketOption() {
        options = new HashMap<>();
    }

    public Set<Map.Entry<ChannelOption<?>, Object>> getSocketOptions() {
        options.put(ChannelOption.TCP_NODELAY, Boolean.FALSE);
        return options.entrySet();
    }
}
