package software.amazon.awssdk.http.nio.netty.internal;

import com.typesafe.netty.http.StreamedHttpRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;

public class DongieOutboundHandler extends ChannelOutboundHandlerAdapter implements Subscriber<HttpContent> {
    private Channel channel;
    private Subscription subscription;
    private final AtomicLong written = new AtomicLong(0);

    @Override
    public void write(ChannelHandlerContext handlerContext, Object msg, ChannelPromise channelPromise) {
        this.channel = handlerContext.channel();
        //System.out.println("Writing msg: " + msg.getClass().getSimpleName());
        if (msg instanceof StreamedHttpRequest) {
            StreamedHttpRequest req = (StreamedHttpRequest) msg;
            //System.out.println("Request: " + req);
            handlerContext.write(req, channelPromise);
            req.subscribe(this);
        } else {
            handlerContext.write(msg, channelPromise);
            if (msg instanceof HttpContent) {
                written.addAndGet(((HttpContent) msg).content().readableBytes());
            }
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        //System.out.println("onSubscribe called...");
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(HttpContent httpContent) {
        //System.out.println("onNext called...");
        channel.writeAndFlush(httpContent);
        written.addAndGet(httpContent.content().readableBytes());
        //System.out.printf("Wrote %d bytes so far%n", written.longValue());
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {
        //System.out.println("onComplete called...");
        channel.writeAndFlush(new DefaultLastHttpContent());
    }
}
