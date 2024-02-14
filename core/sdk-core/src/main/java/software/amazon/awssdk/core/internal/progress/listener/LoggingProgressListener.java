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

package software.amazon.awssdk.core.internal.progress.listener;

import static software.amazon.awssdk.utils.StringUtils.repeat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.utils.Logger;

/**
 * An implementation of {@link ProgressListener} that logs a progress bar at the {@code INFO} level for upload operations. This
 * implementation effectively limits the frequency of updates by limiting logging to events of progress advancement. By default,
 * the progress bar has {@value #DEFAULT_MAX_TICKS} ticks, meaning an update is logged for every 5% progression, at most.
 */
@SdkPublicApi
public final class LoggingProgressListener implements ProgressListener {
    private static final Logger log = Logger.loggerFor(LoggingProgressListener.class);
    private static final int DEFAULT_MAX_TICKS = 20;
    private final ProgressBar progressBar;

    private LoggingProgressListener(int maxTicks) {
        progressBar = new ProgressBar(maxTicks);
    }

    /**
     * Create an instance of {@link LoggingProgressListener} with a custom {@code maxTicks} value.
     *
     * @param maxTicks the number of ticks in the logged progress bar
     */
    public static LoggingProgressListener create(int maxTicks) {
        return new LoggingProgressListener(maxTicks);
    }

    /**
     * Create an instance of {@link LoggingProgressListener} with the default configuration.
     */
    public static LoggingProgressListener create() {
        return new LoggingProgressListener(DEFAULT_MAX_TICKS);
    }


    @Override
    public void requestPrepared(Context.RequestPrepared context) {
        log.info(() -> "Request Prepared...");
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void requestHeaderSent(Context.RequestHeaderSent context) {
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void requestBytesSent(Context.RequestBytesSent context) {
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void responseHeaderReceived(Context.ResponseHeaderReceived context) {
        log.info(() -> "Upload Successful! Starting Download...");
        context.downloadProgressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void responseBytesReceived(Context.ResponseBytesReceived context) {
        context.downloadProgressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void executionSuccess(Context.ExecutionSuccess context) {
        log.info(() -> "Execution Successful!");
        context.downloadProgressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void executionFailure(Context.ExecutionFailure context) {
        log.warn(() -> "Execution Failed!", context.exception());
    }

    private static class ProgressBar {
        private final int maxTicks;
        private final AtomicInteger prevTicks = new AtomicInteger(-1);

        private ProgressBar(int maxTicks) {
            this.maxTicks = maxTicks;
        }

        void update(double ratio) {
            int ticks = (int) Math.floor(ratio * maxTicks);
            if (prevTicks.getAndSet(ticks) != ticks) {
                log.info(() -> String.format("|%s%s| %s",
                                             repeat("=", ticks),
                                             repeat(" ", maxTicks - ticks),
                                             round(ratio * 100, 1) + "%"));
            }
        }

        private static double round(double value, int places) {
            BigDecimal bd = BigDecimal.valueOf(value);
            bd = bd.setScale(places, RoundingMode.FLOOR);
            return bd.doubleValue();
        }
    }

}
