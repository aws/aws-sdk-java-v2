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

package software.amazon.awssdk.transfer.s3.internal;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.jimfs.Jimfs;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;

@WireMockTest
public class MultipartPauseResumeDownloadTest {

    private S3TransferManager tm;
    private FileSystem jimfs;

    private int partSize = 128;

    @BeforeEach
    void init(WireMockRuntimeInfo wm) {
        tm = S3TransferManager.builder()
                              .s3Client(S3AsyncClient.builder()
                                                     .multipartEnabled(true)
                                                     .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                                     .build())
                              .build();
        jimfs = Jimfs.newFileSystem();
    }

    @Test
    void pauseDownloadDuringFirstPart_resumeShouldRestartFromFirstPart() {
        FileDownload dl = tm.downloadFile(DownloadFileRequest.builder()
                                                             .getObjectRequest(r -> r.key("test-key").bucket("test-bucket"))
                                                             .destination(jimfs.getPath("/file", "path"))
                                                             .build());
    }

    // using 16 parts
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16})
    void pauseDownloadMidPart_shouldResumeFromLastUncompletedPart(int partDuringWhichToPause) {

    }


    class TestAsyncResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> {
        private AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> delegate;
        private int pauseDuringPart;

        public TestAsyncResponseTransformer(
            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> delegate, int pauseDuringPart) {
            this.delegate = delegate;
            this.pauseDuringPart = pauseDuringPart;
        }

        @Override
        public CompletableFuture<GetObjectResponse> prepare() {
            return delegate.prepare();
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            delegate.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(s -> publisher.subscribe(new PausingSubscriber(s, pauseDuringPart * partSize)));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            delegate.exceptionOccurred(error);
        }
    }

    class PausingSubscriber implements Subscriber<ByteBuffer> {
        private int pauseAfter;
        private Subscriber<? super ByteBuffer> delegate;
        private int totalRead = 0;

        public PausingSubscriber(Subscriber<? super ByteBuffer> delegate, int pauseAfter) {
            this.pauseAfter = pauseAfter;
            this.delegate = delegate;
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            totalRead += byteBuffer.remaining();
            if (totalRead > pauseAfter) {

            }
            delegate.onNext(byteBuffer);
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }
}
