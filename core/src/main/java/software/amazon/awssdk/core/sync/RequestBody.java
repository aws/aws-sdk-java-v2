/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.sync;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.Validate.paramNotNull;
import static software.amazon.awssdk.utils.Validate.validState;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Represents the body of an HTTP request. Must be provided for operations that have a streaming input.
 * Offers various convenience factory methods from common sources of data (File, String, byte[], etc). Body contents
 * are made reproducible where possible to facilitate automatic retries.
 */
public class RequestBody {

    // TODO reproducible content
    // TODO Handle stream management (progress listener, orig input stream tracking, etc
    private final InputStream inputStream;
    private final long contentLength;

    private RequestBody(InputStream inputStream, long contentLength) {
        this.inputStream = paramNotNull(inputStream, "contents");
        this.contentLength = contentLength;
        validState(contentLength >= 0, "Content length must be greater than or equal to zero");
    }

    /**
     * @return RequestBody as an {@link InputStream}.
     */
    public InputStream asStream() {
        return inputStream;
    }

    /**
     * @return Content length of {@link RequestBody}.
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * Create a {@link RequestBody} using the full contents of the specified file.
     *
     * @param path File to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody of(Path path) {
        return of(path.toFile());
    }

    /**
     * Create a {@link RequestBody} using the full contents of the specified file.
     *
     * @param file File to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody of(File file) {
        return new RequestBody(invokeSafely(() -> new FileInputStream(file)), file.length());
    }

    /**
     * Creates a {@link RequestBody} from an input stream. {@value software.amazon.awssdk.http.Headers#CONTENT_LENGTH} must
     * be provided so that the SDK does not have to make two passes of the data.
     *
     * <p>This stream will be consumed and closed by the SDK. It should NOT be re-used after making an API call with it. It
     * should also not be read outside of the SDK (by another thread) as it will change the state of the {@link InputStream} and
     * could tamper with the sending of the request.</p>
     *
     * @param inputStream   Input stream to send to the service.
     * @param contentLength Content length of data in input stream.
     * @return RequestBody instance.
     */
    public static RequestBody of(InputStream inputStream, long contentLength) {
        return new RequestBody(inputStream, contentLength);
    }

    /**
     * Creates a {@link RequestBody} from a string. String is sent as UTF-8 encoded bytes.
     *
     * @param contents String to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody of(String contents) {
        return RequestBody.of(contents.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a {@link RequestBody} from a byte array. The contents of the byte array are copied so modifications to the
     * original byte array are not reflected in the {@link RequestBody}.
     *
     * @param bytes The bytes to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody of(byte[] bytes) {
        return ofByteDirect(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Creates a {@link RequestBody} from a {@link ByteBuffer}. Buffer contents are copied so any modifications
     * made to the original {@link ByteBuffer} are not reflected in the {@link RequestBody}.
     *
     * @param byteBuffer ByteBuffer to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody of(ByteBuffer byteBuffer) {
        return ofByteDirect(BinaryUtils.copyAllBytesFrom(byteBuffer));
    }

    /**
     * Creates a {@link RequestBody} with no content.
     *
     * @return RequestBody instance.
     */
    public static RequestBody empty() {
        return ofByteDirect(new byte[0]);
    }

    /**
     * Creates a {@link RequestBody} using the specified bytes (without copying).
     */
    private static RequestBody ofByteDirect(byte[] bytes) {
        return new RequestBody(new ByteArrayInputStream(bytes), bytes.length);
    }

}
