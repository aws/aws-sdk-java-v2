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

package software.amazon.awssdk.http.apache5.internal;

import static software.amazon.awssdk.http.Header.CHUNKED;
import static software.amazon.awssdk.http.Header.TRANSFER_ENCODING;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.HttpEntityWrapper;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.utils.Logger;

/**
 * Custom implementation of HttpEntity that delegates to an
 * InputStreamEntity, with the one notable difference, that if
 * the underlying InputStream supports being reset, this RequestEntity will
 * report that it is repeatable and will reset the stream on all subsequent
 * attempts to write out the request.
 */
@SdkInternalApi
public class RepeatableInputStreamRequestEntity extends HttpEntityWrapper {

    private static final Logger log = Logger.loggerFor(RepeatableInputStreamRequestEntity.class);

    /**
     * True if the request entity hasn't been written out yet
     */
    private boolean firstAttempt = true;

    /**
     * True if the "Transfer-Encoding:chunked" header is present
     */
    private boolean isChunked;

    /**
     * The underlying InputStreamEntity being delegated to
     */
    private InputStreamEntity inputStreamRequestEntity;

    /**
     * The InputStream containing the content to write out
     */
    private InputStream content;

    /**
     * Record the original exception if we do attempt a retry, so that if the
     * retry fails, we can report the original exception. Otherwise, we're most
     * likely masking the real exception with an error about not being able to
     * reset far enough back in the input stream.
     */
    private IOException originalException;

    /**
     * Creates a new RepeatableInputStreamRequestEntity using the information
     * from the specified request. If the input stream containing the request's
     * contents is repeatable, then this RequestEntity will report as being
     * repeatable.
     *
     * @param request The details of the request being written out (content type,
     *                content length, and content).
     */
    public RepeatableInputStreamRequestEntity(final HttpExecuteRequest request) {
        super(createInputStreamEntity(request));

        isChunked = request.httpRequest().matchingHeaders(TRANSFER_ENCODING).contains(CHUNKED);

        /*
         * If we don't specify a content length when we instantiate our
         * InputStreamRequestEntity, then HttpClient will attempt to
         * buffer the entire stream contents into memory to determine
         * the content length.
         */
        long contentLength = request.httpRequest().firstMatchingHeader("Content-Length")
                                    .map(this::parseContentLength)
                                    .orElse(-1L);

        content = getContent(request.contentStreamProvider());

        // Create InputStreamEntity with proper ContentType handling for HttpClient 5.x
        ContentType contentType = request.httpRequest().firstMatchingHeader("Content-Type")
                                         .map(this::parseContentType)
                                         .orElse(null);

        if (contentLength >= 0) {
            inputStreamRequestEntity = new InputStreamEntity(content, contentLength, contentType);
        } else {
            inputStreamRequestEntity = new InputStreamEntity(content, contentType);
        }
    }

    private static InputStreamEntity createInputStreamEntity(HttpExecuteRequest request) {
        InputStream content = getContent(request.contentStreamProvider());

        long contentLength = request.httpRequest().firstMatchingHeader("Content-Length")
                                    .map(RepeatableInputStreamRequestEntity::parseContentLengthStatic)
                                    .orElse(-1L);

        ContentType contentType = request.httpRequest().firstMatchingHeader("Content-Type")
                                         .map(RepeatableInputStreamRequestEntity::parseContentTypeStatic)
                                         .orElse(null);

        if (contentLength >= 0) {
            return new InputStreamEntity(content, contentLength, contentType);
        } else {
            return new InputStreamEntity(content, contentType);
        }
    }

    private long parseContentLength(String contentLength) {
        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException nfe) {
            log.warn(() -> "Unable to parse content length from request. Buffering contents in memory.");
            return -1;
        }
    }

    private static long parseContentLengthStatic(String contentLength) {
        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException nfe) {
            log.warn(() -> "Unable to parse content length from request. Buffering contents in memory.");
            return -1;
        }
    }

    private ContentType parseContentType(String contentTypeValue) {
        if (contentTypeValue == null) {
            return null;
        }
        try {
            return ContentType.parse(contentTypeValue);
        } catch (Exception e) {
            log.warn(() -> "Unable to parse content type: " + contentTypeValue);
            return null;
        }
    }

    private static ContentType parseContentTypeStatic(String contentTypeValue) {
        if (contentTypeValue == null) {
            return null;
        }
        try {
            return ContentType.parse(contentTypeValue);
        } catch (Exception e) {
            log.warn(() -> "Unable to parse content type: " + contentTypeValue);
            return null;
        }
    }

    /**
     * @return The request content input stream or an empty input stream if there is no content.
     */
    private static InputStream getContent(Optional<ContentStreamProvider> contentStreamProvider) {
        return contentStreamProvider.map(ContentStreamProvider::newStream)
                                    .orElseGet(() -> new ByteArrayInputStream(new byte[0]));
    }

    @Override
    public boolean isChunked() {
        return isChunked;
    }

    /**
     * Returns true if the underlying InputStream supports marking/resetting or
     * if the underlying InputStreamRequestEntity is repeatable.
     */
    @Override
    public boolean isRepeatable() {
        boolean markSupported = content.markSupported();
        boolean entityRepeatable = inputStreamRequestEntity.isRepeatable();
        boolean result = markSupported || entityRepeatable;
        return result;
    }


    /**
     * Resets the underlying InputStream if this isn't the first attempt to
     * write out the request, otherwise simply delegates to
     * InputStreamRequestEntity to write out the data.
     * <p>
     * If an error is encountered the first time we try to write the request
     * entity, we remember the original exception, and report that as the root
     * cause if we continue to encounter errors, rather than masking the
     * original error.
     */
    @Override
    public void writeTo(OutputStream output) throws IOException {
        try {
            if (!firstAttempt && isRepeatable()) {
                content.reset();
            }

            firstAttempt = false;
            inputStreamRequestEntity.writeTo(output);
        } catch (IOException ioe) {
            if (originalException == null) {
                originalException = ioe;
            }
            throw originalException;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (content != null) {
                content.close();
            }
        } finally {
            super.close();
        }
    }
}
