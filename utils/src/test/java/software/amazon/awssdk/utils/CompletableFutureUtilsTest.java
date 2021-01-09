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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import org.junit.Test;

public class CompletableFutureUtilsTest {

    @Test(timeout = 1000)
    public void testForwardException() {
        CompletableFuture src = new CompletableFuture();
        CompletableFuture dst = new CompletableFuture();

        Exception e = new RuntimeException("BOOM");

        CompletableFutureUtils.forwardExceptionTo(src, dst);

        src.completeExceptionally(e);

        try {
            dst.join();
            fail();
        } catch (Throwable t) {
            assertThat(t.getCause()).isEqualTo(e);
        }
    }

    @Test(timeout = 1000)
    public void anyFail_shouldCompleteWhenAnyFutureFails() {
        RuntimeException exception = new RuntimeException("blah");
        CompletableFuture[] completableFutures = new CompletableFuture[2];
        completableFutures[0] = new CompletableFuture();
        completableFutures[1] = new CompletableFuture();

        CompletableFuture<Void> anyFail = CompletableFutureUtils.anyFail(completableFutures);
        completableFutures[0] = CompletableFuture.completedFuture("test");
        completableFutures[1].completeExceptionally(exception);
        assertThat(anyFail.isDone()).isTrue();
    }

    @Test(timeout = 1000)
    public void anyFail_shouldNotCompleteWhenAllFuturesSucceed() {
        CompletableFuture[] completableFutures = new CompletableFuture[2];
        completableFutures[0] = new CompletableFuture();
        completableFutures[1] = new CompletableFuture();

        CompletableFuture<Void> anyFail = CompletableFutureUtils.anyFail(completableFutures);
        completableFutures[0] = CompletableFuture.completedFuture("test");
        completableFutures[1] = CompletableFuture.completedFuture("test");
        assertThat(anyFail.isDone()).isFalse();
    }

    @Test(timeout = 1000)
    public void allOfCancelForwarded_anyFutureFails_shouldCancelOthers() {
        RuntimeException exception = new RuntimeException("blah");
        CompletableFuture[] completableFutures = new CompletableFuture[2];
        completableFutures[0] = new CompletableFuture();
        completableFutures[1] = new CompletableFuture();

        CompletableFuture<Void> resultFuture = CompletableFutureUtils.allOfCancelForwarded(completableFutures);
        completableFutures[0].completeExceptionally(exception);

        assertThatThrownBy(resultFuture::join).hasCause(exception);

        assertThat(completableFutures[1].isCancelled()).isTrue();
    }

    @Test(timeout = 1000)
    public void allOfCancelForwarded_allFutureSucceed_shouldComplete() {
        RuntimeException exception = new RuntimeException("blah");
        CompletableFuture[] completableFutures = new CompletableFuture[2];
        completableFutures[0] = new CompletableFuture();
        completableFutures[1] = new CompletableFuture();

        CompletableFuture<Void> resultFuture = CompletableFutureUtils.allOfCancelForwarded(completableFutures);
        completableFutures[0].complete("test");
        completableFutures[1].complete("test");

        assertThat(resultFuture.isDone()).isTrue();
        assertThat(resultFuture.isCompletedExceptionally()).isFalse();
    }
}
