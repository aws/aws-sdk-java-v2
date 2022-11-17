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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompletableFutureUtilsTest {
    private static ExecutorService executors;

    @BeforeClass
    public static void setup() {
        executors = Executors.newFixedThreadPool(2);
    }

    @AfterClass
    public static void tearDown() {
        executors.shutdown();
    }

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
    public void forwardResultTo_srcCompletesSuccessfully_shouldCompleteDstFuture() {
        CompletableFuture<String> src = new CompletableFuture<>();
        CompletableFuture<String> dst = new CompletableFuture<>();

        CompletableFuture<String> returnedFuture = CompletableFutureUtils.forwardResultTo(src, dst, executors);
        assertThat(returnedFuture).isEqualTo(src);

        src.complete("foobar");
        assertThat(dst.join()).isEqualTo("foobar");
    }

    @Test(timeout = 1000)
    public void forwardResultTo_srcCompletesExceptionally_shouldCompleteDstFuture() {
        CompletableFuture<String> src = new CompletableFuture<>();
        CompletableFuture<String> dst = new CompletableFuture<>();

        RuntimeException exception = new RuntimeException("foobar");
        CompletableFutureUtils.forwardResultTo(src, dst, executors);

        src.completeExceptionally(exception);
        assertThatThrownBy(dst::join).hasCause(exception);
    }

    @Test(timeout = 1000)
    public void forwardTransformedResultTo_srcCompletesSuccessfully_shouldCompleteDstFuture() {
        CompletableFuture<Integer> src = new CompletableFuture<>();
        CompletableFuture<String> dst = new CompletableFuture<>();

        CompletableFuture<Integer> returnedFuture = CompletableFutureUtils.forwardTransformedResultTo(src, dst, String::valueOf);
        assertThat(returnedFuture).isSameAs(src);

        src.complete(123);
        assertThat(dst.join()).isEqualTo("123");
    }

    @Test(timeout = 1000)
    public void forwardTransformedResultTo_srcCompletesExceptionally_shouldCompleteDstFuture() {
        CompletableFuture<Integer> src = new CompletableFuture<>();
        CompletableFuture<String> dst = new CompletableFuture<>();

        RuntimeException exception = new RuntimeException("foobar");
        CompletableFutureUtils.forwardTransformedResultTo(src, dst, String::valueOf);

        src.completeExceptionally(exception);
        assertThatThrownBy(dst::join).hasCause(exception);
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
        assertThatThrownBy(anyFail::join).hasCause(exception);
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
    public void allOfExceptionForwarded_anyFutureFails_shouldForwardExceptionToOthers() {
        RuntimeException exception = new RuntimeException("blah");
        CompletableFuture[] completableFutures = new CompletableFuture[2];
        completableFutures[0] = new CompletableFuture();
        completableFutures[1] = new CompletableFuture();

        CompletableFuture<Void> resultFuture = CompletableFutureUtils.allOfExceptionForwarded(completableFutures);
        completableFutures[0].completeExceptionally(exception);

        assertThatThrownBy(resultFuture::join).hasCause(exception);
        assertThatThrownBy(completableFutures[1]::join).hasCause(exception);
    }

    @Test(timeout = 1000)
    public void allOfExceptionForwarded_allFutureSucceed_shouldComplete() {
        RuntimeException exception = new RuntimeException("blah");
        CompletableFuture[] completableFutures = new CompletableFuture[2];
        completableFutures[0] = new CompletableFuture();
        completableFutures[1] = new CompletableFuture();

        CompletableFuture<Void> resultFuture = CompletableFutureUtils.allOfExceptionForwarded(completableFutures);
        completableFutures[0].complete("test");
        completableFutures[1].complete("test");

        assertThat(resultFuture.isDone()).isTrue();
        assertThat(resultFuture.isCompletedExceptionally()).isFalse();
    }
}
