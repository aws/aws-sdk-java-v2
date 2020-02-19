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
 *
 * Original source licensed under the Apache License 2.0 by playframework.
 */

package software.amazon.awssdk.http.nio.netty.internal.nrs;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import software.amazon.awssdk.http.nio.netty.internal.nrs.util.BatchedProducer;
import software.amazon.awssdk.http.nio.netty.internal.nrs.util.ClosedLoopChannel;
import software.amazon.awssdk.http.nio.netty.internal.nrs.util.ScheduledBatchedProducer;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
public class HandlerPublisherVerificationTest extends PublisherVerification<Long> {

    private final int batchSize;
    // The number of elements to publish initially, before the subscriber is received
    private final int publishInitial;
    // Whether we should use scheduled publishing (with a small delay)
    private final boolean scheduled;

    private ScheduledExecutorService executor;
    private DefaultEventLoopGroup eventLoop;

    @Factory(dataProvider = "data")
    public HandlerPublisherVerificationTest(int batchSize, int publishInitial, boolean scheduled) {
        super(new TestEnvironment(200));
        this.batchSize = batchSize;
        this.publishInitial = publishInitial;
        this.scheduled = scheduled;
    }

    @DataProvider
    public static Object[][] data() {
        final int defaultBatchSize = 3;
        final int defaultPublishInitial = 3;
        final boolean defaultScheduled = false;

        return new Object[][] {
            { defaultBatchSize, defaultPublishInitial, defaultScheduled },
            { 1, defaultPublishInitial, defaultScheduled },
            { defaultBatchSize, 0, defaultScheduled },
            { defaultBatchSize, defaultPublishInitial, true }
        };
    }

    // I tried making this before/after class, but encountered a strange error where after 32 publishers were created,
    // the following tests complained about the executor being shut down when I registered the channel. Though, it
    // doesn't happen if you create 32 publishers in a single test.
    @BeforeMethod
    public void startEventLoop() {
        eventLoop = new DefaultEventLoopGroup();
    }

    @AfterMethod
    public void stopEventLoop() {
        eventLoop.shutdownGracefully();
        eventLoop = null;
    }

    @BeforeClass
    public void startExecutor() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterClass
    public void stopExecutor() {
        executor.shutdown();
    }

    @Override
    public Publisher<Long> createPublisher(final long elements) {
        final BatchedProducer out;
        if (scheduled) {
            out = new ScheduledBatchedProducer(elements, batchSize, publishInitial, executor, 5);
        } else {
            out = new BatchedProducer(elements, batchSize, publishInitial, executor);
        }

        final ClosedLoopChannel channel = new ClosedLoopChannel();
        channel.config().setAutoRead(false);
        ChannelFuture registered = eventLoop.register(channel);

        final HandlerPublisher<Long> publisher = new HandlerPublisher<>(registered.channel().eventLoop(), Long.class);

        registered.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channel.pipeline().addLast("out", out);
                channel.pipeline().addLast("publisher", publisher);

                for (long i = 0; i < publishInitial && i < elements; i++) {
                    channel.pipeline().fireChannelRead(i);
                }
                if (elements <= publishInitial) {
                    channel.pipeline().fireChannelInactive();
                }
            }
        });

        return publisher;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        LocalChannel channel = new LocalChannel();
        eventLoop.register(channel);
        HandlerPublisher<Long> publisher = new HandlerPublisher<>(channel.eventLoop(), Long.class);
        channel.pipeline().addLast("publisher", publisher);
        channel.pipeline().fireExceptionCaught(new RuntimeException("failed"));

        return publisher;
    }

    @Override
    public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
        try {
            super.stochastic_spec103_mustSignalOnMethodsSequentially();
        } catch (Throwable t) {
            // CI is failing here, but maven doesn't tell us which parameters failed
            System.out.println("Stochastic test failed with parameters batchSize=" + batchSize +
                               " publishInitial=" + publishInitial + " scheduled=" + scheduled);
            throw t;
        }
    }
}
