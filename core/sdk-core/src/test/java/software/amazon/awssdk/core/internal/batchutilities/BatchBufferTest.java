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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

public class BatchBufferTest {

    private BatchBuffer<String, String, List<String>> buffer;
    private String destination;
    private int currentId;

    BiFunction<Map<String, String>, String, CompletableFuture<List<String>>> batchingFunction =
        (requestEntryMap, destination) -> {
            List<String> entries = new ArrayList<>(requestEntryMap.size());
            requestEntryMap.forEach((key, value) -> entries.add(value));
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return entries;
            });
        };

    Function<List<String>, Map<String, String>> unpackResponseFunction =
        requestBatchResponse -> {
            Map<String, String> mappedResponses = new HashMap<>();
            for (int i = 0; i < requestBatchResponse.size(); i++) {
                mappedResponses.put(Integer.toString(currentId++), requestBatchResponse.get(i));
            }
            return mappedResponses;
        };

    @Before
    public void beforeEachBufferTest() {
        buffer = new BatchBuffer<>(10, Duration.ofMillis(200), batchingFunction, unpackResponseFunction);
        destination = "testDestination";
        currentId = Integer.MIN_VALUE;
    }

    @Test
    public void sendTenRequestsTest() {
        String[] requests = new String[10];
        for (int i = 0; i < requests.length; i++) {
            requests[i] = Integer.toString(i);
        }
        List<CompletableFuture<String>> responses = new ArrayList<>();
        for (String request : requests) {
            responses.add(buffer.sendRequest(request, destination));
        }
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        for (int i = 0; i < requests.length; i++) {
            Assert.assertEquals(responses.get(i).join(), Integer.toString(i));
        }
    }

    @Test
    public void sendTwentyRequestsTest() {
        String[] requests = new String[20];
        for (int i = 0; i < requests.length; i++) {
            requests[i] = Integer.toString(i);
        }
        List<CompletableFuture<String>> responses = new ArrayList<>();
        for (String request : requests) {
            responses.add(buffer.sendRequest(request, destination));
        }
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        for (CompletableFuture<String> response: responses) {
            System.out.println("Message body:" + response.join());
        }
    }
}
