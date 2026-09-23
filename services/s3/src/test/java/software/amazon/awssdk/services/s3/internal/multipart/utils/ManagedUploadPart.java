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

package software.amazon.awssdk.services.s3.internal.multipart.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.Pair;

/**
 * Records the consumer and pending future of each UploadPart request so a test can complete parts
 * the same way MultipartUploadHelper does: consumer.accept(completedPart) followed by future completion.
 *
 * <p>Intended to be used as a Mockito {@link Answer} for
 * {@code MultipartUploadHelper#sendIndividualUploadPartRequest}.
 */
public final class ManagedUploadPart implements Answer<CompletableFuture<CompletedPart>> {
    private final Map<Integer, Consumer<CompletedPart>> consumers = new HashMap<>();
    private final Map<Integer, CompletableFuture<CompletedPart>> futures = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<CompletedPart> answer(InvocationOnMock invocation) {
        Consumer<CompletedPart> consumer = invocation.getArgument(1, Consumer.class);
        Pair<UploadPartRequest, AsyncRequestBody> pair = invocation.getArgument(3, Pair.class);
        int partNumber = pair.left().partNumber();
        CompletableFuture<CompletedPart> future = new CompletableFuture<>();
        consumers.put(partNumber, consumer);
        futures.put(partNumber, future);
        return future;
    }

    public void completePart(int partNumber) {
        CompletableFuture<CompletedPart> future = futures.get(partNumber);
        assertThat(future).withFailMessage("UploadPart request for part %d was never sent", partNumber).isNotNull();
        CompletedPart part = CompletedPart.builder().partNumber(partNumber).eTag("etag-" + partNumber).build();
        consumers.get(partNumber).accept(part);
        future.complete(part);
    }
}
