package software.amazon.awssdk.http.nio.netty.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static software.amazon.awssdk.http.nio.netty.internal.utils.SocketChannelResolver.resolveSocketChannelClass;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import org.junit.Test;
import software.amazon.awssdk.http.nio.netty.internal.DelegatingEventLoopGroup;

public class SocketChannelResolverTest {

    @Test
    public void canDetectForStandardNioEventLoopGroup() {
        assertThat(resolveSocketChannelClass(new NioEventLoopGroup())).isEqualTo(NioSocketChannel.class);
    }

    @Test
    public void canDetectEpollEventLoopGroup() {
        assumeTrue(Epoll.isAvailable());
        assertThat(resolveSocketChannelClass(new EpollEventLoopGroup())).isEqualTo(EpollSocketChannel.class);
    }

    @Test
    public void worksWithDelegateEventLoopGroups() {
        assertThat(resolveSocketChannelClass(new DelegatingEventLoopGroup(new NioEventLoopGroup()) {})).isEqualTo(NioSocketChannel.class);
    }

    @Test
    public void worksWithOioEventLoopGroup() {
        assertThat(resolveSocketChannelClass(new OioEventLoopGroup())).isEqualTo(OioSocketChannel.class);
    }
}