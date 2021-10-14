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

/**
 * An example implementation of {@link TransferListener} that prints a progress bar to System.out. This example is referenced to
 * in the {@link TransferListener} documentation and also used in {@link S3TransferManager} integration tests.
 */
public class ProgressPrintingTransferListener implements TransferListener {
    
    private final ProgressBar progressBar = new ProgressBar(20);

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        System.out.println("Transfer initiated...");
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
        System.out.println("Transfer complete!");
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        System.out.println("Transfer failed.");
        context.exception().printStackTrace();
    }

    private static class ProgressBar {
        private final int maxTicks;
        private int prevTicks = -1;

        public ProgressBar(int maxTicks) {
            this.maxTicks = maxTicks;
        }

        public void update(double ratio) {
            int ticks = (int) Math.floor(ratio * maxTicks);
            if (ticks != prevTicks) {
                System.out.printf("|%s%s| %s%n",
                                  repeat("=", ticks),
                                  repeat(" ", maxTicks - ticks),
                                  round(ratio * 100, 1) + "%");
                prevTicks = ticks;
            }
        }

        private static String repeat(String str, int n) {
            return String.join("", Collections.nCopies(n, str));
        }

        private static double round(double value, int places) {
            BigDecimal bd = BigDecimal.valueOf(value);
            bd = bd.setScale(places, RoundingMode.HALF_DOWN);
            return bd.doubleValue();
        }
    }
}
