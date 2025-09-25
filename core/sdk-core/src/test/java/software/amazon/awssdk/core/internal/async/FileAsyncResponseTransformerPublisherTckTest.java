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

package software.amazon.awssdk.core.internal.async;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

public class FileAsyncResponseTransformerPublisherTckTest extends PublisherVerification<AsyncResponseTransformer<SdkResponse,
    SdkResponse>> {

    private final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    private final Path testFile = fileSystem.getPath("/test-file.txt");


    public FileAsyncResponseTransformerPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<AsyncResponseTransformer<SdkResponse, SdkResponse>> createPublisher(long elements) {
        FileAsyncResponseTransformer<?> art =
            (FileAsyncResponseTransformer<?>) AsyncResponseTransformer.toFile(testFile);
        FileAsyncResponseTransformerPublisher<SdkResponse> publisher =
            new FileAsyncResponseTransformerPublisher<>(art);

        return SdkPublisher.adapt(publisher).limit((int) elements);
    }

    @Override
    public Publisher<AsyncResponseTransformer<SdkResponse, SdkResponse>> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return Long.MAX_VALUE;
    }

}
