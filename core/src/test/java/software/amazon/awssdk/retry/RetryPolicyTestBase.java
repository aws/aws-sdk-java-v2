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

package software.amazon.awssdk.retry;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.util.StringInputStream;

/**
 * Some utility class and method for testing RetryCondition.
 */
public class RetryPolicyTestBase {

    protected static final AmazonWebServiceRequest originalRequest = new TestAmazonWebServiceRequest();
    protected static final HttpResponseHandler<AmazonServiceException> errorResponseHandler = new TestHttpResponseHandler();

    /**
     * The retry condition and back-off strategy implementations that record all
     * the context data passed into shouldRetry and calculateSleepTime methods.
     */
    protected static ContextDataCollectionRetryCondition retryCondition;
    protected static ContextDataCollectionBackoffStrategy backoffStrategy;

    @SuppressWarnings("rawtypes")
    public static Request<?> getSampleRequestWithRepeatableContent(AmazonWebServiceRequest amazonWebServiceRequest) {
        DefaultRequest<?> request = new DefaultRequest(
                amazonWebServiceRequest, "non-existent-service");
        request.setEndpoint(URI.create("http://non-existent-service.amazonaws.com"));
        // StringInputStream#markSupported() returns true
        try {
            request.setContent(new StringInputStream("Some content that could be read for multiple times."));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("Unable to set up the request content");
        }
        return request;
    }

    @SuppressWarnings("rawtypes")
    public static Request<?> getSampleRequestWithNonRepeatableContent(AmazonWebServiceRequest amazonWebServiceRequest) {
        DefaultRequest<?> request = new DefaultRequest(
                amazonWebServiceRequest, "non-existent-service");
        request.setEndpoint(URI.create("http://non-existent-service.amazonaws.com"));
        // NonRepeatableInputStream#markSupported() returns false
        request.setContent(new NonRepeatableInputStream("Some content that could only be read once."));
        return request;
    }

    /**
     * Verifies the RetryCondition has collected the expected context information.
     */
    public static void verifyExpectedContextData(ContextDataCollection contextDataCollection,
                                                 AmazonWebServiceRequest failedRequest,
                                                 AmazonClientException expectedException,
                                                 int expectedRetries) {

        Assert.assertEquals(expectedRetries, contextDataCollection.failedRequests.size());
        Assert.assertEquals(expectedRetries, contextDataCollection.exceptions.size());
        Assert.assertEquals(expectedRetries, contextDataCollection.retriesAttemptedValues.size());

        if (expectedRetries > 0) {
            if (failedRequest != null) {
                // It should keep getting the same original request instance
                for (AmazonWebServiceRequest seenRequest : contextDataCollection.failedRequests) {
                    Assert.assertTrue("seeRequest=" + seenRequest
                                      + ", failedRequest=" + failedRequest,
                                      seenRequest == failedRequest);
                }
            }

            // Verify the exceptions
            if (expectedException instanceof AmazonServiceException) {
                // It should get service exceptions with the expected error and status code
                AmazonServiceException ase = (AmazonServiceException) expectedException;
                for (AmazonClientException seenException : contextDataCollection.exceptions) {
                    Assert.assertTrue(seenException instanceof AmazonServiceException);
                    Assert.assertEquals(ase.getErrorCode(), ((AmazonServiceException) seenException).getErrorCode());
                    Assert.assertEquals(ase.getStatusCode(), ((AmazonServiceException) seenException).getStatusCode());
                }
            } else if (expectedException != null) {
                // Client exceptions should have the same expected cause (the same
                // throwable instance from the mock HttpClient).
                Throwable expectedCause = expectedException.getCause();
                for (AmazonClientException seenException : contextDataCollection.exceptions) {
                    Assert.assertTrue(expectedCause == seenException.getCause());
                }
            }

            // It should get "retriesAttempted" values starting from 0
            int expectedRetriesAttempted = 0;
            for (int seenRetriesAttempted : contextDataCollection.retriesAttemptedValues) {
                Assert.assertEquals(expectedRetriesAttempted++, seenRetriesAttempted);
            }
        }

    }

    public static class ContextDataCollectionRetryCondition extends
                                                            ContextDataCollection implements RetryPolicy.RetryCondition {

        @Override
        public boolean shouldRetry(AmazonWebServiceRequest originalRequest,
                                   AmazonClientException exception,
                                   int retriesAttempted) {
            collect(originalRequest, exception, retriesAttempted);
            return true;
        }
    }

    public static class ContextDataCollectionBackoffStrategy extends
                                                             ContextDataCollection implements RetryPolicy.BackoffStrategy {

        @Override
        public long delayBeforeNextRetry(AmazonWebServiceRequest originalRequest,
                                         AmazonClientException exception,
                                         int retriesAttempted) {
            collect(originalRequest, exception, retriesAttempted);
            return 0; // immediately retry to speed-up the test
        }
    }

    private static class ContextDataCollection {

        public List<AmazonWebServiceRequest> failedRequests = new LinkedList<AmazonWebServiceRequest>();
        public List<AmazonClientException> exceptions = new LinkedList<AmazonClientException>();
        public List<Integer> retriesAttemptedValues = new LinkedList<Integer>();

        public void collect(AmazonWebServiceRequest originalRequest,
                            AmazonClientException exception, int retriesAttempted) {
            failedRequests.add(originalRequest);
            exceptions.add(exception);
            retriesAttemptedValues.add(retriesAttempted);
        }
    }

    public static class TestAmazonWebServiceRequest extends AmazonWebServiceRequest {
    }

    /**
     * An error response handler implementation that simply
     * - keeps the status code
     * - sets the error code by the status text (which comes from the reason phrase in the low-level response)
     */
    public static class TestHttpResponseHandler implements HttpResponseHandler<AmazonServiceException> {

        @Override
        public AmazonServiceException handle(
                HttpResponse response,
                ExecutionAttributes executionAttributes) throws Exception {
            AmazonServiceException ase = new AmazonServiceException("Fake service exception.");
            ase.setStatusCode(response.getStatusCode());
            ase.setErrorCode(response.getStatusText());
            return ase;
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return false;
        }
    }

}
