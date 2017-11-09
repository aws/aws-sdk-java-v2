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

package software.amazon.awssdk.core.retry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.AmazonClientException;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.http.AmazonHttpClient;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.AmazonWebServiceRequestAdapter;
import software.amazon.awssdk.core.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import utils.HttpTestUtils;

/**
 * Tests that {@link AmazonHttpClient} passes the correct context information into the configured RetryPolicy.
 */
@RunWith(MockitoJUnitRunner.class)
public class AmazonHttpClientRetryPolicyTest extends RetryPolicyTestBase {

    private static final int EXPECTED_RETRY_COUNT = 5;
    private AmazonHttpClient testedClient;

    @Mock
    private SdkHttpClient sdkHttpClient;

    @Mock
    private AbortableCallable<SdkHttpFullResponse> abortableCallable;

    /**
     * Reset the RetryPolicy and restart collecting context data.
     */
    @Before
    public void resetContextData() {
        retryCondition = new ContextDataCollectionRetryCondition();
        backoffStrategy = new ContextDataCollectionBackoffStrategy();
        // Reset the RetryPolicy
        RetryPolicy retryPolicy = new RetryPolicy(retryCondition,
                                                  backoffStrategy,
                                                  EXPECTED_RETRY_COUNT, // max error retry
                                                  false);

        when(sdkHttpClient.prepareRequest(any(), any())).thenReturn(abortableCallable);
        testedClient = HttpTestUtils.testClientBuilder()
                                    .httpClient(sdkHttpClient)
                                    .retryPolicy(retryPolicy)
                                    .build();
    }

    /**
     * Tests AmazonHttpClient's behavior upon simulated service exceptions when the
     * request payload is repeatable.
     */
    @Test
    public void testServiceExceptionHandling() throws Exception {
        int statusCode = 500;
        String statusText = "InternalServerError";
        // A mock HttpClient that always returns the specified status and error code.
        when(abortableCallable.call()).thenReturn(SdkHttpFullResponse.builder()
                                                                     .statusCode(statusCode)
                                                                     .statusText(statusText)
                                                                     .build());

        Request<?> testedRepeatableRequest = getSampleRequestWithRepeatableContent(originalRequest);
        ExecutionContext context = createExecutionContext(originalRequest);

        // It should keep retrying until it reaches the max retry limit and
        // throws the simulated ASE.
        AmazonServiceException expectedServiceException = null;
        try {
            testedClient.requestExecutionBuilder()
                        .request(testedRepeatableRequest)
                        .requestConfig(new AmazonWebServiceRequestAdapter(originalRequest))
                        .errorResponseHandler(errorResponseHandler)
                        .executionContext(context)
                        .execute();
            Assert.fail("AmazonServiceException is expected.");
        } catch (AmazonServiceException ase) {
            // We should see the original service exception
            assertEquals(statusCode, ase.getStatusCode());
            assertEquals(statusText, ase.getErrorCode());
            expectedServiceException = ase;
        }

        // Verifies that the correct information was passed into the RetryCondition and BackoffStrategy
        verifyExpectedContextData(retryCondition,
                                  originalRequest,
                                  expectedServiceException,
                                  EXPECTED_RETRY_COUNT);
        verifyExpectedContextData(backoffStrategy,
                                  originalRequest,
                                  expectedServiceException,
                                  EXPECTED_RETRY_COUNT);

        // request count = retries + 1
        verify(abortableCallable, new Times(EXPECTED_RETRY_COUNT + 1)).call();
    }

    /**
     * Tests AmazonHttpClient's behavior upon simulated IOException during
     * executing the http request when the request payload is repeatable.
     */
    @Test
    public void testIoExceptionHandling() throws Exception {
        // A mock HttpClient that always throws the specified IOException object
        IOException simulatedIoException = new IOException("fake IOException");

        when(abortableCallable.call()).thenThrow(simulatedIoException);

        Request<?> testedRepeatableRequest = getSampleRequestWithRepeatableContent(originalRequest);
        ExecutionContext context = createExecutionContext(originalRequest);

        // It should keep retrying until it reaches the max retry limit and
        // throws the an ACE containing the simulated IOException.
        AmazonClientException expectedClientException = null;
        try {
            testedClient.requestExecutionBuilder()
                        .request(testedRepeatableRequest)
                        .requestConfig(new AmazonWebServiceRequestAdapter(originalRequest))
                        .errorResponseHandler(errorResponseHandler)
                        .executionContext(context)
                        .execute();
            Assert.fail("AmazonClientException is expected.");
        } catch (AmazonClientException ace) {
            Assert.assertTrue(simulatedIoException == ace.getCause());
            expectedClientException = ace;
        }

        // Verifies that the correct information was passed into the RetryCondition and BackoffStrategy
        verifyExpectedContextData(retryCondition,
                                  originalRequest,
                                  expectedClientException,
                                  EXPECTED_RETRY_COUNT);
        verifyExpectedContextData(backoffStrategy,
                                  originalRequest,
                                  expectedClientException,
                                  EXPECTED_RETRY_COUNT);

        // request count = retries + 1
        verify(abortableCallable, new Times(EXPECTED_RETRY_COUNT + 1)).call();
    }

    /**
     * Tests AmazonHttpClient's behavior upon simulated service exceptions when the
     * request payload is not repeatable.
     */
    @Test
    public void testServiceExceptionHandlingWithNonRepeatableRequestContent() throws Exception {
        int statusCode = 513;
        String statusText = "SomeError";

        when(abortableCallable.call()).thenReturn(SdkHttpFullResponse.builder()
                                                                     .statusCode(statusCode)
                                                                     .statusText(statusText)
                                                                     .build());

        // A non-repeatable request
        Request<?> testedNonRepeatableRequest = getSampleRequestWithNonRepeatableContent(originalRequest);
        ExecutionContext context = createExecutionContext(originalRequest);

        // It should fail directly and throw the ASE, without consulting the
        // custom shouldRetry(..) method.
        try {
            testedClient.requestExecutionBuilder()
                        .request(testedNonRepeatableRequest)
                        .errorResponseHandler(errorResponseHandler)
                        .executionContext(context)
                        .execute();
            Assert.fail("AmazonServiceException is expected.");
        } catch (AmazonServiceException ase) {
            assertEquals(statusCode, ase.getStatusCode());
            assertEquals(statusText, ase.getErrorCode());
        }

        // Verifies that shouldRetry and calculateSleepTime were never called
        verifyExpectedContextData(retryCondition,
                                  null,
                                  null,
                                  EXPECTED_RETRY_COUNT);
        verifyExpectedContextData(backoffStrategy,
                                  null,
                                  null,
                                  EXPECTED_RETRY_COUNT);
        // request count = retries + 1
        verify(abortableCallable, new Times(EXPECTED_RETRY_COUNT + 1)).call();
    }

    /**
     * Tests AmazonHttpClient's behavior upon simulated IOException when the
     * request payload is not repeatable.
     */
    @Test
    public void testIoExceptionHandlingWithNonRepeatableRequestContent() throws Exception {
        // A mock HttpClient that always throws the specified IOException object
        IOException simulatedIOException = new IOException("fake IOException");

        when(abortableCallable.call()).thenThrow(simulatedIOException);

        // A non-repeatable request
        Request<?> testedRepeatableRequest = getSampleRequestWithNonRepeatableContent(originalRequest);
        ExecutionContext context = createExecutionContext(originalRequest);

        // It should fail directly and throw an ACE containing the simulated
        // IOException, without consulting the
        // custom shouldRetry(..) method.
        try {
            testedClient.requestExecutionBuilder()
                        .request(testedRepeatableRequest)
                        .errorResponseHandler(errorResponseHandler)
                        .executionContext(context)
                        .execute();
            Assert.fail("AmazonClientException is expected.");
        } catch (AmazonClientException ace) {
            Assert.assertTrue(simulatedIOException == ace.getCause());
        }

        // Verifies that shouldRetry and calculateSleepTime are still called
        verifyExpectedContextData(retryCondition,
                                  null,
                                  null,
                                  EXPECTED_RETRY_COUNT);
        verifyExpectedContextData(backoffStrategy,
                                  null,
                                  null,
                                  EXPECTED_RETRY_COUNT);

        // request count = retries + 1
        verify(abortableCallable, new Times(EXPECTED_RETRY_COUNT + 1)).call();
    }

    /**
     * Tests AmazonHttpClient's behavior upon simulated RuntimeException (which
     * should be handled as an unexpected failure and not retried).
     */
    @Test
    public void testUnexpectedFailureHandling() throws Exception {
        // A mock HttpClient that always throws an NPE
        NullPointerException simulatedNPE = new NullPointerException("fake NullPointerException");
        when(abortableCallable.call()).thenThrow(simulatedNPE);

        // The OldExecutionContext should collect the expected RequestCount
        Request<?> testedRepeatableRequest = getSampleRequestWithRepeatableContent(originalRequest);
        ExecutionContext context = createExecutionContext(originalRequest);

        // It should fail directly and throw the simulated NPE, without
        // consulting the custom shouldRetry(..) method.
        try {
            testedClient.requestExecutionBuilder()
                        .request(testedRepeatableRequest)
                        .errorResponseHandler(errorResponseHandler)
                        .executionContext(context)
                        .execute();
            Assert.fail("AmazonClientException is expected.");
        } catch (NullPointerException npe) {
            Assert.assertTrue(simulatedNPE == npe);
        }

        // Verifies that shouldRetry and calculateSleepTime were never called
        verifyExpectedContextData(retryCondition,
                                  null,
                                  null,
                                  0);
        verifyExpectedContextData(backoffStrategy,
                                  null,
                                  null,
                                  0);

        // The captured RequestCount should be 1
        verify(abortableCallable, new Times(1)).call();
    }

    private ExecutionContext createExecutionContext(SdkRequest request) {
        return ExecutionContext.builder()
                               .signerProvider(new NoOpSignerProvider())
                               .interceptorChain(new ExecutionInterceptorChain(Collections.emptyList()))
                               .executionAttributes(new ExecutionAttributes())
                               .interceptorContext(InterceptorContext.builder().request(request).build())
                               .build();
    }
}
