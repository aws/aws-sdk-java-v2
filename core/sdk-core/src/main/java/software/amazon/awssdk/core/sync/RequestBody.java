/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.sync.FileContentStreamProvider;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.io.ReleasableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Represents the body of an HTTP request. Must be provided for operations that have a streaming input.
 * Offers various convenience factory methods from common sources of data (File, String, byte[], etc).
 */
@SdkPublicApi
public final class RequestBody {

    // TODO Handle stream management (progress listener, orig input stream tracking, etc
    private final ContentStreamProvider contentStreamProvider;
    private final long contentLength;
    private final String contentType;

    private RequestBody(ContentStreamProvider contentStreamProvider, long contentLength, String contentType) {
        this.contentStreamProvider = paramNotNull(contentStreamProvider, "contentStreamProvider");
        this.contentLength = contentLength;
        this.contentType = paramNotNull(contentType, "contentType");
        validState(contentLength >= 0, "Content length must be greater than or equal to zero");
    }

    /**
     * @return RequestBody as an {@link InputStream}.
     */
    public ContentStreamProvider contentStreamProvider() {
        return contentStreamProvider;
    }

    /**
     * @return Content length of {@link RequestBody}.
     */
    public long contentLength() {
        return contentLength;
    }

    /**
     * @return Content type of {@link RequestBody}.
     */
    public String contentType() {
        return contentType;
    }

    /**
     * Create a {@link RequestBody} using the full contents of the specified file.
     *
     * @param path File to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody fromFile(Path path) {
        return new RequestBody(new FileContentStreamProvider(path),
                               invokeSafely(() -> Files.size(path)),
                               Mimetype.getInstance().getMimetype(path));
    }

    /**
     * Create a {@link RequestBody} using the full contents of the specified file.
     *
     * @param file File to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody fromFile(File file) {
        return fromFile(file.toPath());
    }

    /**
     * Creates a {@link RequestBody} from an input stream. {@value Header#CONTENT_LENGTH} must
     * be provided so that the SDK does not have to make two passes of the data.
     * <p>
     * The stream will not be closed by the SDK. It is up to to caller of this method to close the stream. The stream
     * should not be read outside of the SDK (by another thread) as it will change the state of the {@link InputStream} and
     * could tamper with the sending of the request.
     * <p>
     * To support resetting via {@link ContentStreamProvider}, this uses {@link InputStream#reset()} and uses a read limit of
     * 128 KiB. If you need more control, use {@link #fromContentProvider(ContentStreamProvider, long, String)}.
     *
     * @param inputStream   Input stream to send to the service. The stream will not be closed by the SDK.
     * @param contentLength Content length of data in input stream.
     * @return RequestBody instance.
     */
    public static RequestBody fromInputStream(InputStream inputStream, long contentLength) {
        IoUtils.markStreamWithMaxReadLimit(inputStream);
        InputStream nonCloseable = nonCloseableInputStream(inputStream);
        return fromContentProvider(() -> {
            if (nonCloseable.markSupported()) {
                invokeSafely(nonCloseable::reset);
            }
            return nonCloseable;
        }, contentLength, Mimetype.MIMETYPE_OCTET_STREAM);
    }

    /**
     * Creates a {@link RequestBody} from a string. String is sent using the provided encoding.
     *
     * @param contents String to send to the service.
     * @param cs The {@link Charset} to use.
     * @return RequestBody instance.
     */
    public static RequestBody fromString(String contents, Charset cs) {
        return fromBytesDirect(contents.getBytes(cs), Mimetype.MIMETYPE_TEXT_PLAIN);
    }

    /**
     * Creates a {@link RequestBody} from a string. String is sent as UTF-8 encoded bytes.
     *
     * @param contents String to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody fromString(String contents) {
        return fromString(contents, StandardCharsets.UTF_8);
    }

    /**
     * Creates a {@link RequestBody} from a byte array. The contents of the byte array are copied so modifications to the
     * original byte array are not reflected in the {@link RequestBody}.
     *
     * @param bytes The bytes to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody fromBytes(byte[] bytes) {
        return fromBytesDirect(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Creates a {@link RequestBody} from a {@link ByteBuffer}. Buffer contents are copied so any modifications
     * made to the original {@link ByteBuffer} are not reflected in the {@link RequestBody}.
     *
     * @param byteBuffer ByteBuffer to send to the service.
     * @return RequestBody instance.
     */
    public static RequestBody fromByteBuffer(ByteBuffer byteBuffer) {
        return fromBytesDirect(BinaryUtils.copyAllBytesFrom(byteBuffer));
    }

    /**
     * Creates a {@link RequestBody} with no content.
     *
     * @return RequestBody instance.
     */
    public static RequestBody empty() {
        return fromBytesDirect(new byte[0]);
    }

    /**
     * Creates a {@link RequestBody} from the given {@link ContentStreamProvider}.
     *
     * @param provider The content provider.
     * @param contentLength The content length.
     * @param mimeType The MIME type of the content.
     *
     * @return The created {@code RequestBody}.
     */
    public static RequestBody fromContentProvider(ContentStreamProvider provider, long contentLength, String mimeType) {
        return new RequestBody(provider, contentLength, mimeType);
    }

    /**
     * Creates a {@link RequestBody} using the specified bytes (without copying).
     */
    private static RequestBody fromBytesDirect(byte[] bytes) {
        return fromBytesDirect(bytes, Mimetype.MIMETYPE_OCTET_STREAM);
    }

    /**
     * Creates a {@link RequestBody} using the specified bytes (without copying).
     */
    private static RequestBody fromBytesDirect(byte[] bytes, String mimetype) {
        return fromContentProvider(() -> new ByteArrayInputStream(bytes), bytes.length, mimetype);
    }

    private static InputStream nonCloseableInputStream(InputStream inputStream) {
        return inputStream != null ? ReleasableInputStream.wrap(inputStream).disableClose()
                                   : null;
    }
}
