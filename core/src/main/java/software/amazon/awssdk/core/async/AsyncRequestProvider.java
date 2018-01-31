/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;

/**
 * Interface to allow non-blocking streaming of request content. This follows the reactive streams pattern where
 * this interface is the {@link Publisher} of data (specifically {@link ByteBuffer} chunks) and the HTTP client is the Subscriber
 * of the data (i.e. to write that data on the wire).
 *
 * <p>
 * {@link #subscribe(Subscriber)} should be implemented to tie this publisher to a subscriber. Ideally each call to subscribe
 * should reproduce the content (i.e if you are reading from a file each subscribe call should produce a {@link
 * org.reactivestreams.Subscription} that reads the file fully). This allows for automatic retries to be performed in the SDK. If
 * the content is not reproducible,  an exception may be thrown from any subsequent {@link #subscribe(Subscriber)} calls.
 * </p>
 *
 * <p>
 * It is important to only send the number of chunks that the subscriber requests to avoid out of memory situations.
 * The subscriber does it's own buffering so it's usually not needed to buffer in the publisher. Additional permits
 * for chunks will be notified via the {@link org.reactivestreams.Subscription#request(long)} method.
 * </p>
 *
 * @see FileAsyncRequestProvider
 */
@ReviewBeforeRelease("This is exactly the same of SdkHttpRequestProvider. Can we just have one? Also I like Producer better" +
                     "than Provider")
public interface AsyncRequestProvider extends Publisher<ByteBuffer> {

    /**
     * @return The content length of the data being produced.
     */
    long contentLength();

    /**
     * Creates an {@link AsyncRequestProvider} that produces data from the contents of a file. See
     * {@link FileAsyncRequestProvider#builder} to create a customized provider implementation.
     *
     * @param path Path to file to read from.
     * @return Implementation of {@link AsyncRequestProvider} that reads data from the specified file.
     * @see FileAsyncRequestProvider
     */
    static AsyncRequestProvider fromFile(Path path) {
        return FileAsyncRequestProvider.builder().path(path).build();
    }

    /**
     * Creates an {@link AsyncRequestProvider} that uses a single string as data.
     *
     * @param string The string to provide.
     * @param cs The {@link Charset} to use.
     * @return Implementation of {@link AsyncRequestProvider} that uses the specified string.
     * @see SingleByteArrayAsyncRequestProvider
     */
    static AsyncRequestProvider fromString(String string, Charset cs) {
        return new SingleByteArrayAsyncRequestProvider(string.getBytes(cs));
    }

    /**
     * Creates an {@link AsyncRequestProvider} that uses a single string as data with UTF_8 encoding.
     *
     * @param string The string to provider.
     * @return Implementation of {@link AsyncRequestProvider} that uses the specified string.
     * @see #fromString(String, Charset)
     */
    static AsyncRequestProvider fromString(String string) {
        return fromString(string, StandardCharsets.UTF_8);
    }

}
