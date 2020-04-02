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

package software.amazon.awssdk.awscore.presigner;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.Validate;


/**
 * The base class for all presigned requests.
 * <p/>
 * The {@link #isBrowserExecutable} method can be used to determine whether this request can be executed by a web browser.
 */
@SdkPublicApi
public abstract class PresignedRequest {
    private final URL url;
    private final Instant expiration;
    private final boolean isBrowserExecutable;
    private final Map<String, List<String>> signedHeaders;
    private final SdkBytes signedPayload;
    private final SdkHttpRequest httpRequest;

    protected PresignedRequest(DefaultBuilder<?> builder) {
        this.expiration = Validate.notNull(builder.expiration, "expiration");
        this.isBrowserExecutable = Validate.notNull(builder.isBrowserExecutable, "isBrowserExecutable");
        this.signedHeaders = Validate.notEmpty(builder.signedHeaders, "signedHeaders");
        this.signedPayload = builder.signedPayload;
        this.httpRequest = Validate.notNull(builder.httpRequest, "httpRequest");
        this.url = invokeSafely(httpRequest.getUri()::toURL);
    }

    /**
     * The URL that the presigned request will execute against. The {@link #isBrowserExecutable} method can be used to
     * determine whether this request will work in a browser.
     */
    public URL url() {
        return url;
    }

    /**
     * The exact SERVICE time that the request will expire. After this time, attempting to execute the request
     * will fail.
     * <p/>
     * This may differ from the local clock, based on the skew between the local and AWS service clocks.
     */
    public Instant expiration() {
        return expiration;
    }

    /**
     * Whether the url returned by the url method can be executed in a browser.
     * <p/>
     * This is true when the HTTP request method is GET and all data included in the signature will be sent by a standard web
     * browser.
     */
    public boolean isBrowserExecutable() {
        return isBrowserExecutable;
    }

    /**
     * Returns the subset of headers that were signed, and MUST be included in the presigned request to prevent
     * the request from failing.
     */
    public Map<String, List<String>> signedHeaders() {
        return signedHeaders;
    }

    /**
     * Returns the payload that was signed, or Optional.empty() if there is no signed payload with this request.
     */
    public Optional<SdkBytes> signedPayload() {
        return Optional.ofNullable(signedPayload);
    }

    /**
     * The entire SigV4 query-parameter signed request (minus the payload), that can be transmitted as-is to a
     * service using any HTTP client that implement the SDK's HTTP client SPI.
     * <p>
     * This request includes signed AND unsigned headers.
     */
    public SdkHttpRequest httpRequest() {
        return httpRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PresignedRequest that = (PresignedRequest) o;

        if (isBrowserExecutable != that.isBrowserExecutable) {
            return false;
        }
        if (!expiration.equals(that.expiration)) {
            return false;
        }
        if (!signedHeaders.equals(that.signedHeaders)) {
            return false;
        }
        if (signedPayload != null ? !signedPayload.equals(that.signedPayload) : that.signedPayload != null) {
            return false;
        }
        return httpRequest.equals(that.httpRequest);
    }

    @Override
    public int hashCode() {
        int result = expiration.hashCode();
        result = 31 * result + (isBrowserExecutable ? 1 : 0);
        result = 31 * result + signedHeaders.hashCode();
        result = 31 * result + (signedPayload != null ? signedPayload.hashCode() : 0);
        result = 31 * result + httpRequest.hashCode();
        return result;
    }

    @SdkPublicApi
    public interface Builder {
        /**
         * Configure the exact SERVICE time that the request will expire. After this time, attempting to execute the request
         * will fail.
         */
        Builder expiration(Instant expiration);

        /**
         * Configure whether the url returned by the url method can be executed in a browser.
         */
        Builder isBrowserExecutable(Boolean isBrowserExecutable);

        /**
         * Configure the subset of headers that were signed, and MUST be included in the presigned request to prevent
         * the request from failing.
         */
        Builder signedHeaders(Map<String, List<String>> signedHeaders);

        /**
         * Configure the payload that was signed.
         */
        Builder signedPayload(SdkBytes signedPayload);

        /**
         * Configure the entire SigV4 query-parameter signed request (minus the payload), that can be transmitted as-is to a
         * service using any HTTP client that implement the SDK's HTTP client SPI.
         */
        Builder httpRequest(SdkHttpRequest httpRequest);

        PresignedRequest build();
    }



    @SdkProtectedApi
    protected abstract static class DefaultBuilder<B extends DefaultBuilder<B>> implements Builder {
        private Instant expiration;
        private Boolean isBrowserExecutable;
        private Map<String, List<String>> signedHeaders;
        private SdkBytes signedPayload;
        private SdkHttpRequest httpRequest;

        protected DefaultBuilder() {
        }

        protected DefaultBuilder(PresignedRequest request) {
            this.expiration = request.expiration;
            this.isBrowserExecutable = request.isBrowserExecutable;
            this.signedHeaders = request.signedHeaders;
            this.signedPayload = request.signedPayload;
            this.httpRequest = request.httpRequest;
        }

        @Override
        public B expiration(Instant expiration) {
            this.expiration = expiration;
            return thisBuilder();
        }

        @Override
        public B isBrowserExecutable(Boolean isBrowserExecutable) {
            this.isBrowserExecutable = isBrowserExecutable;
            return thisBuilder();
        }

        @Override
        public B signedHeaders(Map<String, List<String>> signedHeaders) {
            this.signedHeaders = signedHeaders;
            return thisBuilder();
        }

        @Override
        public B signedPayload(SdkBytes signedPayload) {
            this.signedPayload = signedPayload;
            return thisBuilder();
        }

        @Override
        public B httpRequest(SdkHttpRequest httpRequest) {
            this.httpRequest = httpRequest;
            return thisBuilder();
        }

        @SuppressWarnings("unchecked")
        private B thisBuilder() {
            return (B) this;
        }
    }
}