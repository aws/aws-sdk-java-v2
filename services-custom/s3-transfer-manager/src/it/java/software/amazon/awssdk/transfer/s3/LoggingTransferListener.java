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

package software.amazon.awssdk.transfer.s3;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example implementation of {@link TransferListener} that logs a progress bar at the INFO level. This example is referenced to
 * in the {@link TransferListener} documentation and also used in {@link S3TransferManager} integration tests.
 */
public class LoggingTransferListener implements TransferListener {
    private static final Logger log = LoggerFactory.getLogger(LoggingTransferListener.class);
    private final ProgressBar progressBar = new ProgressBar(20);

    private LoggingTransferListener() {
    }

    public static LoggingTransferListener create() {
        return new LoggingTransferListener();
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        log.info("Transfer initiated...");
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
        log.info("Transfer complete!");
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        log.warn("Transfer failed.", context.exception());
    }

    private static class ProgressBar {
        private final int maxTicks;
        private final AtomicInteger prevTicks = new AtomicInteger(-1);

        public ProgressBar(int maxTicks) {
            this.maxTicks = maxTicks;
        }

        public void update(double ratio) {
            int ticks = (int) Math.floor(ratio * maxTicks);
            if (prevTicks.getAndSet(ticks) != ticks) {
                log.info("|{}{}| {}",
                         repeat("=", ticks),
                         repeat(" ", maxTicks - ticks),
                         round(ratio * 100, 1) + "%");
            }
        }

        private static String repeat(String str, int n) {
            return String.join("", Collections.nCopies(n, str));
        }

        private static double round(double value, int places) {
            BigDecimal bd = BigDecimal.valueOf(value);
            bd = bd.setScale(places, RoundingMode.FLOOR);
            return bd.doubleValue();
        }
    }
}
