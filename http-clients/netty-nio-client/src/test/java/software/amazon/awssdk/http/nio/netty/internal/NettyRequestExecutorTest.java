package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Promise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.utils.AttributeMap;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NettyRequestExecutorTest {

    private ChannelPool mockChannelPool;

    private EventLoopGroup eventLoopGroup;

    private NettyRequestExecutor nettyRequestExecutor;

    private RequestContext requestContext;

    @Before
    public void setup() {
        mockChannelPool = mock(ChannelPool.class);

        eventLoopGroup = new NioEventLoopGroup();

        requestContext = new RequestContext(mockChannelPool,
                                            eventLoopGroup,
                                            AsyncExecuteRequest.builder().build(),
                                            new NettyConfiguration(AttributeMap.empty()));
        nettyRequestExecutor = new NettyRequestExecutor(requestContext);
    }

    @After
    public void teardown() throws InterruptedException {
        eventLoopGroup.shutdownGracefully().await();
    }

    @Test
    public void cancelExecuteFuture_channelNotAcquired_failsAcquirePromise() {
        ArgumentCaptor<Promise> acquireCaptor = ArgumentCaptor.forClass(Promise.class);
        when(mockChannelPool.acquire(acquireCaptor.capture())).thenAnswer((Answer<Promise>) invocationOnMock -> {
            return invocationOnMock.getArgumentAt(0, Promise.class);
        });

        CompletableFuture<Void> executeFuture = nettyRequestExecutor.execute();

        executeFuture.cancel(true);

        assertThat(acquireCaptor.getValue().isDone()).isTrue();
        assertThat(acquireCaptor.getValue().isSuccess()).isFalse();
    }

    @Test
    public void cancelExecuteFuture_channelAcquired_submitsRunnable() {
        EventLoop mockEventLoop = mock(EventLoop.class);
        Channel mockChannel = mock(Channel.class);
        when(mockChannel.eventLoop()).thenReturn(mockEventLoop);

        when(mockChannelPool.acquire(any(Promise.class))).thenAnswer((Answer<Promise>) invocationOnMock -> {
            Promise p = invocationOnMock.getArgumentAt(0, Promise.class);
            p.setSuccess(mockChannel);
            return p;
        });

        CompletableFuture<Void> executeFuture = nettyRequestExecutor.execute();

        executeFuture.cancel(true);

        verify(mockEventLoop).submit(any(Runnable.class));
    }
}
