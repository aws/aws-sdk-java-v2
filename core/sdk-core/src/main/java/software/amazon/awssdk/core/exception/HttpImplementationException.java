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

package software.amazon.awssdk.core.exception;

public class HttpImplementationException extends SdkClientException {

    private static final long serialVersionUID = 1L;

    private HttpImplementationException(HttpImplementationException.Builder b) {
        super(b);
    }

    public static HttpImplementationException create(String message) {
        return builder().message(message).build();
    }

    public static HttpImplementationException create(String message, Throwable cause) {
        return builder().message(message).cause(cause).build();
    }

    @Override
    public HttpImplementationException.Builder toBuilder() {
        return new HttpImplementationException.BuilderImpl(this);
    }

    public static HttpImplementationException.Builder builder() {
        return new HttpImplementationException.BuilderImpl();
    }

    public interface Builder extends SdkClientException.Builder {
        @Override
        HttpImplementationException.Builder message(String message);

        @Override
        HttpImplementationException.Builder cause(Throwable cause);

        @Override
        HttpImplementationException.Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        HttpImplementationException build();
    }

    protected static final class BuilderImpl extends SdkClientException.BuilderImpl implements HttpImplementationException.Builder {

        protected BuilderImpl() {
        }

        protected BuilderImpl(HttpImplementationException ex) {
            super(ex);
        }

        @Override
        public HttpImplementationException.Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public HttpImplementationException.Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public HttpImplementationException.Builder writableStackTrace(Boolean writableStackTrace) {
            this.writableStackTrace = writableStackTrace;
            return this;
        }

        @Override
        public HttpImplementationException build() {
            return new HttpImplementationException(this);
        }
    }
}