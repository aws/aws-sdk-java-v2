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

package software.amazon.awssdk.core.internal.batchutilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class BatchUtilsTest {

    @Test
    public void getAndIncrementIdInSingleThread() {
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            String current = BatchUtils.getAndIncrementId(counter);
            Assert.assertEquals(Integer.toString(i), current);
        }
    }

    @Test
    public void getAndIncrementIdInFiveThreads() {
        int numThreads = 5;
        AtomicInteger counter = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        Set<String> responses = new HashSet<>();
        List<CompletableFuture<Void>> executions = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            executions.add(CompletableFuture.supplyAsync(() -> incrementAtomicInt(counter, responses), executor));
        }

        CompletableFuture.allOf(executions.toArray(new CompletableFuture[0])).join();
        for (int i = 0; i < numThreads * 10; i++) {
            Assert.assertTrue(responses.contains(Integer.toString(i)));
        }
        executor.shutdownNow();
    }

    @Test
    public void getAndIncrementIdHandlingIntegerOverflow() {
        AtomicInteger counter = new AtomicInteger(Integer.MAX_VALUE - 1);
        int temp = Integer.MAX_VALUE - 1;
        for (int i = 0; i < 10; temp++, i++) {
            if (temp == Integer.MAX_VALUE) {
                temp = 0;
            }
            String current = BatchUtils.getAndIncrementId(counter);
            Assert.assertEquals(Integer.toString(temp), current);
        }
    }

    private Void incrementAtomicInt(AtomicInteger atomicInteger, Set<String> responses) {
        for (int i = 0; i < 10; i++) {
            responses.add(BatchUtils.getAndIncrementId(atomicInteger));
        }
        return null;
    }
}
