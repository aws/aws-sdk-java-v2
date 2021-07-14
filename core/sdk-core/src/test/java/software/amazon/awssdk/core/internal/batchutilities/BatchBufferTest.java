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

    private BatchBuffer<String, String, List<RequestWithId>> buffer;
    private String destination;
    private int currentId;

    BatchAndSendFunction<String, List<RequestWithId>> batchingFunction =
        (identifiedRequests, destination) -> {
            List<RequestWithId> entries = new ArrayList<>(identifiedRequests.size());
            identifiedRequests.forEach(identifiedRequest -> {
                String id = identifiedRequest.getId();
                String request = identifiedRequest.getRequest();
                entries.add(new RequestWithId(Integer.parseInt(id), request));
            });
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return entries;
            });
        };

//    Function<List<RequestWithId>, List<IdentifiedResponse<String>>> unpackResponseFunction =
    UnpackBatchResponseFunction<List<RequestWithId>, String> unpackResponseFunction =
        requestBatchResponse -> {
            List<IdentifiedResponse<String>> mappedResponses = new ArrayList<>();
            for (RequestWithId requestWithId : requestBatchResponse) {
                mappedResponses.add(new IdentifiedResponse<>(Integer.toString(currentId++), requestWithId.getMessage()));
            }
            return mappedResponses;
        };

    //Object to mimic a batch request entry
    private static class RequestWithId {

        private final int id;
        private final String message;

        public RequestWithId(int id, String message) {
            this.id = id;
            this.message = message;
        }

        public int getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

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
            requests[i] = "Message " + i;
        }
        List<CompletableFuture<String>> responses = new ArrayList<>();
        for (String request : requests) {
            responses.add(buffer.sendRequest(request, destination));
        }
        CompletableFuture.allOf(responses.toArray(new CompletableFuture[0])).join();
        for (int i = 0; i < requests.length; i++) {
            Assert.assertEquals(responses.get(i).join(), "Message " + i);
        }
    }

    @Test
    public void sendTwentyRequestsTest() {
        String[] requests = new String[20];
        for (int i = 0; i < requests.length; i++) {
            requests[i] = "Message " + i;
        }
        List<CompletableFuture<String>> responses = new ArrayList<>();
        for (String request : requests) {
            responses.add(buffer.sendRequest(request, destination));
        }
        // Sometimes responses are returned out of order which seems to indicate it is not properly correlated.
        // Not entirely sure why it happens sometimes but not other times.
        // Could be how I use assertions or create the objects or even construct the response array? Not entirely sure.
        // Seems to be the first batch response is spliced together with second batch response.
        for (int i = 0; i < requests.length; i++) {
            String response = responses.get(i).join();
            System.out.println(response);
            Assert.assertEquals(response, "Message " + i);
        }
    }
}
