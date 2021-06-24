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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

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
}
