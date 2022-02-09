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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;

/**
 * TCK verification test for {@link DownloadDirectoryHelper.S3ObjectSubscriber}.
 */
public class S3ObjectSubscriberTckTest extends SubscriberWhiteboxVerification<S3Object> {
    private final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());

    protected S3ObjectSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<S3Object> createSubscriber(WhiteboxSubscriberProbe<S3Object> whiteboxSubscriberProbe) {
        Path temp = fs.getPath("/", UUID.randomUUID().toString());
        DownloadDirectoryRequest request = DownloadDirectoryRequest.builder().bucket("bucket").destinationDirectory(temp).build();
        Function<DownloadFileRequest, FileDownload> function = req -> new DefaultFileDownload(new CompletableFuture<>(),
                                                                                              new TransferProgressUpdater(req,
                                                                                                                          null).progress());
        return new DownloadDirectoryHelper.S3ObjectSubscriber(request, new CompletableFuture<>(), function, 1) {

            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                whiteboxSubscriberProbe.registerOnSubscribe(new SubscriberPuppet() {

                    @Override
                    public void triggerRequest(long l) {
                        s.request(l);
                    }

                    @Override
                    public void signalCancel() {
                        s.cancel();
                    }
                });
            }

            @Override
            public void onNext(S3Object bb) {
                super.onNext(bb);
                whiteboxSubscriberProbe.registerOnNext(bb);
            }

            @Override
            public void onError(Throwable t) {
                super.onError(t);
                whiteboxSubscriberProbe.registerOnError(t);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                whiteboxSubscriberProbe.registerOnComplete();
            }
        };
    }

    @Override
    public S3Object createElement(int i) {
        return S3Object.builder().key("key" + UUID.randomUUID()).build();
    }
}
