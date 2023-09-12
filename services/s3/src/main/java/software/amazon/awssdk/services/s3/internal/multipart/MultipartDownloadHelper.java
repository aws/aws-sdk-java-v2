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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class MultipartDownloadHelper<T> {
    private final S3AsyncClient s3AsyncClient;

    private CompletableFuture<T> returnFuture;
    private GetObjectRequest getObjectRequest;
    private AsyncResponseTransformer<GetObjectResponse, T> responseTransformer;
    private boolean useMulti = false;

    MultipartDownloadHelper(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
        this.returnFuture = new CompletableFuture<>();
    }

    public void getObject(GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {
        this.responseTransformer = asyncResponseTransformer;
        this.getObjectRequest = getObjectRequest;

        GetObjectRequest firstPartRequest = getObjectRequest.copy(b -> b.partNumber(1));

        CompletableFuture<T> response = s3AsyncClient.getObject(firstPartRequest, new MultiGetAsyncResponseTransformer());
        response.whenComplete((res, err) -> {
            if (err != null) {
                // todo
            }
        });
    }

    private class MultiGetAsyncResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, T> {
        private int totalParts;
        private AtomicInteger currentPart = new AtomicInteger(1);

        @Override
        public CompletableFuture<T> prepare() {
            return returnFuture;
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            Integer partCount = response.partsCount();
            if (partCount == null || partCount <= 1) {
                // dont use part
                responseTransformer.onResponse(response);
                return;
            }
            useMulti = true;
            // todo use multipart get
            doMultipartDownload();
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            if (!useMulti) {
                responseTransformer.onStream(publisher);
                return;
            }
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            responseTransformer.exceptionOccurred(error);
        }


        private void doMultipartDownload() {

        }

    }

    public CompletableFuture<T> future() {
        return this.returnFuture;
    }



}
