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

package software.amazon.awssdk.http.apache.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;

/**
 * Custom implementation of {@link org.apache.http.HttpEntity} that delegates to an
 * {@link RepeatableInputStreamRequestEntity}, with the one notable difference, that if
 * the underlying InputStream supports being reset, this RequestEntity will
 * report that it is repeatable and will reset the stream on all subsequent
 * attempts to write out the request.
 */
@SdkInternalApi
public class RepeatableInputStreamRequestEntity extends BasicHttpEntity {

    private static final Logger log = LoggerFactory.getLogger(RepeatableInputStreamRequestEntity.class);

    /**
     * True if the request entity hasn't been written out yet
     */
    private boolean firstAttempt = true;

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
        setChunked(false);

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
        // TODO v2 MetricInputStreamEntity
        inputStreamRequestEntity = new InputStreamEntity(content, contentLength);
        setContent(content);
        setContentLength(contentLength);

        request.httpRequest().firstMatchingHeader("Content-Type").ifPresent(contentType -> {
            inputStreamRequestEntity.setContentType(contentType);
            setContentType(contentType);
        });
    }

    private long parseContentLength(String contentLength) {
        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException nfe) {
            log.warn("Unable to parse content length from request. Buffering contents in memory.");
            return -1;
        }
    }

    /**
     * @return The request content input stream or an empty input stream if there is no content.
     */
    private InputStream getContent(Optional<ContentStreamProvider> contentStreamProvider) {
        return contentStreamProvider.map(ContentStreamProvider::newStream).orElseGet(() -> new ByteArrayInputStream(new byte[0]));
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    /**
     * Returns true if the underlying InputStream supports marking/reseting or
     * if the underlying InputStreamRequestEntity is repeatable.
     */
    @Override
    public boolean isRepeatable() {
        return content.markSupported() || inputStreamRequestEntity.isRepeatable();
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

}
