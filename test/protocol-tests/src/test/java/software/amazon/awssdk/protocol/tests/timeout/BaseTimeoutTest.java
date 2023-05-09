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

package software.amazon.awssdk.protocol.tests.timeout;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.assertj.core.api.ThrowableAssert;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;

public abstract class BaseTimeoutTest {

    /**
     * @return the exception to assert
     */
    protected abstract Consumer<ThrowableAssert.ThrowingCallable> timeoutExceptionAssertion();

    protected abstract Consumer<ThrowableAssert.ThrowingCallable> serviceExceptionAssertion();

    protected abstract Callable callable();

    protected abstract Callable retryableCallable();

    protected abstract Callable streamingCallable();

    protected abstract void stubSuccessResponse(Duration delayAfterTimeout);

    protected abstract void stubErrorResponse(Duration delayAfterTimeout);

    protected void verifySuccessResponseNotTimedOut() throws Exception {
        assertThat(callable().call()).isNotNull();
    }

    protected void verifyFailedResponseNotTimedOut() throws Exception {
        serviceExceptionAssertion().accept(() -> callable().call());
    }

    protected void verifyRetraybleFailedResponseNotTimedOut() {
        serviceExceptionAssertion().accept(() -> retryableCallable().call());
    }

    protected void verifyTimedOut() {
        timeoutExceptionAssertion().accept(() -> callable().call());
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    protected void verifyRetryableTimeout() {
        timeoutExceptionAssertion().accept(() -> retryableCallable().call());
    }

    public static class SlowBytesResponseTransformer<ResponseT> implements ResponseTransformer<ResponseT, ResponseBytes<ResponseT>> {

        private ResponseTransformer<ResponseT, ResponseBytes<ResponseT>> delegate;

        public SlowBytesResponseTransformer() {
            this.delegate = ResponseTransformer.toBytes();
        }

        @Override
        public ResponseBytes<ResponseT> transform(ResponseT response, AbortableInputStream inputStream) throws Exception {

            wastingTimeInterruptibly();
            return delegate.transform(response, inputStream);
        }
    }

    public static class SlowInputStreamResponseTransformer<ResponseT> implements ResponseTransformer<ResponseT, ResponseInputStream<ResponseT>> {

        private ResponseTransformer<ResponseT, ResponseInputStream<ResponseT>> delegate;

        public SlowInputStreamResponseTransformer() {
            this.delegate = ResponseTransformer.toInputStream();
        }

        @Override
        public ResponseInputStream<ResponseT> transform(ResponseT response, AbortableInputStream inputStream) throws Exception {
            wastingTimeInterruptibly();
            return delegate.transform(response, inputStream);
        }
    }

    public static class SlowFileResponseTransformer<ResponseT> implements ResponseTransformer<ResponseT, ResponseT> {

        private ResponseTransformer<ResponseT, ResponseT> delegate;

        public SlowFileResponseTransformer() {
            try {
                this.delegate = ResponseTransformer.toFile(File.createTempFile("ApiCallTiemoutTest", ".txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public ResponseT transform(ResponseT response, AbortableInputStream inputStream) throws Exception {
            wastingTimeInterruptibly();
            return delegate.transform(response, inputStream);
        }
    }

    public static class SlowCustomResponseTransformer implements ResponseTransformer {

        @Override
        public Object transform(Object response, AbortableInputStream inputStream) throws Exception {
            wastingTimeInterruptibly();
            return null;
        }
    }

    public static void wastingTimeInterruptibly() throws InterruptedException {
        Thread.sleep(1200);
    }

    public abstract MockHttpClient mockHttpClient();

    public static HttpExecuteResponse mockResponse(int statusCode) {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(statusCode)
                                                           .build())
                                  .build();
    }

    public void verifyRequestCount(int requestCount) {
        assertThat(mockHttpClient().getRequests().size()).isEqualTo(requestCount);
    }
}
