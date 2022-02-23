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

package software.amazon.awssdk.http.urlconnection.internal.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.urlconnection.internal.UrlConnectionLogger;
import software.amazon.awssdk.utils.Logger;

/**
 * An implementation of {@link HttpConnection} that handles a bug in {@link HttpURLConnection#getOutputStream()} and
 * {@link HttpURLConnection#getInputStream()} where these methods will throw a ProtocolException if we sent an "Expect:
 * 100-continue" header, and the service responds with something other than a 100.
 *
 * {@code HttpURLConnection} still gives us access to the response code and headers when this bug is encountered, so our
 * handling of the bug is:
 * <ol>
 *     <li>If the service returned a response status or content length that indicates there was no response payload,
 *     we ignore that we couldn't read the response payload, and just return the response with what we have.</li>
 *     <li>If the service returned a payload and we can't read it because of the bug, we throw an exception for
 *     non-failure cases (2xx, 3xx) or log and return the response without the payload for failure cases (4xx or 5xx)
 *     .</li>
 * </ol>
 */
@SdkInternalApi
public class Expect100BugHttpConnection extends DelegatingHttpConnection {
    private static final Logger log = UrlConnectionLogger.LOG;
    private final SdkHttpRequest request;

    /**
     * Whether we encountered the 'bug' in the way the HttpURLConnection handles 'Expect: 100-continue' cases. See
     * {@link #getAndHandle100Bug} for more information.
     */
    private boolean expect100BugEncountered = false;

    /**
     * Result cache for {@link #responseHasNoContent()}.
     */
    private Boolean responseHasNoContent;

    public Expect100BugHttpConnection(HttpConnection delegate, SdkHttpRequest request) {
        super(delegate);
        this.request = request;
    }

    @Override
    public OutputStream getRequestStream() {
        return getAndHandle100Bug(delegate::getRequestStream, false);
    }

    @Override
    public InputStream getResponseStream() {
        return getAndHandle100Bug(delegate::getResponseStream, true);
    }

    @Override
    public InputStream getResponseErrorStream() {
        InputStream result = delegate.getResponseErrorStream();
        if (result == null && expect100BugEncountered) {
            log.debug(() -> "The response payload has been dropped because of a limitation of the JDK's URL Connection "
                            + "HTTP client, resulting in a less descriptive SDK exception error message. Using "
                            + "the Apache HTTP client removes this limitation.");
        }
        return result;
    }

    private <T> T getAndHandle100Bug(Supplier<T> supplier, boolean failOn100Bug) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            if (!exceptionCausedBy100HandlingBug(e)) {
                throw e;
            }

            if (responseHasNoContent()) {
                return null;
            }

            expect100BugEncountered = true;

            if (!failOn100Bug) {
                return null;
            }

            int responseCode = delegate.getResponseCode();
            String message = "Unable to read response payload, because service returned response code "
                             + responseCode + " to an Expect: 100-continue request. Using another HTTP client "
                             + "implementation (e.g. Apache) removes this limitation.";
            throw new UncheckedIOException(new IOException(message, e));
        }
    }

    private boolean exceptionCausedBy100HandlingBug(RuntimeException e) {
        return requestWasExpect100Continue() &&
               e.getMessage() != null &&
               e.getMessage().startsWith("java.net.ProtocolException: Server rejected operation");
    }

    private Boolean requestWasExpect100Continue() {
        return request.firstMatchingHeader("Expect")
                      .map(expect -> expect.equalsIgnoreCase("100-continue"))
                      .orElse(false);
    }

    private boolean responseHasNoContent() {
        // We cannot account for chunked encoded responses, because we only have access to headers and response code here,
        // so we assume chunked encoded responses DO have content.
        if (responseHasNoContent == null) {
            responseHasNoContent = responseNeverHasPayload() ||
                                   Objects.equals(delegate.getResponseHeader("Content-Length"), "0") ||
                                   request.method() == SdkHttpMethod.HEAD;
        }
        return responseHasNoContent;
    }

    private boolean responseNeverHasPayload() {
        int responseCode = delegate.getResponseCode();
        return responseCode == 204 || responseCode == 304 || (responseCode >= 100 && responseCode < 200);
    }
}
