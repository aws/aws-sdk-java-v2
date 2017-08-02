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

package software.amazon.awssdk.services.dynamodb;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressListener.ExceptionReporter;
import software.amazon.awssdk.event.ProgressTracker;
import software.amazon.awssdk.event.SdkProgressPublisher;
import software.amazon.awssdk.event.request.Progress;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.RetryPolicy;
import software.amazon.awssdk.retry.RetryPolicyAdapter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.test.util.ProgressListenerWithEventCodeVerification;
import software.amazon.awssdk.util.ImmutableMapParameter;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.BasicTempTable;
import utils.test.util.DynamoDBTestBase;

@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
                            @RequiredResource(resource = BasicTempTable.class,
                                              creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                                              retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS)
                    })
public class RequestProgressIntegrationTest extends DynamoDBTestBase {
    private static final long KB = 1024;

    private static BatchWriteItemRequest generateLargeBatchWriteItemRequest() {
        List<WriteRequest> writes = new LinkedList<WriteRequest>();
        for (int i = 0; i < 25; i++) {
            writes.add(WriteRequest.builder().putRequest(PutRequest.builder().item(
                    ImmutableMapParameter.of(
                            BasicTempTable.HASH_KEY_NAME, AttributeValue.builder().s(Integer.toString(i)).build(),
                            "large-random-string", AttributeValue.builder().s(RandomStringGenerator.nextRandomString(40 * KB)).build())).build()).build());
        }
        return BatchWriteItemRequest.builder().requestItems(
                Collections.singletonMap(BasicTempTable.TEMP_TABLE_NAME, writes)).build();
    }

    private static void waitTillListenerCallbacksComplete() {
        try {
            SdkProgressPublisher.waitTillCompletion();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted when waiting for the progress listener callbacks to return. "
                        + e.getMessage());
        } catch (ExecutionException e) {
            Assert.fail("Error when executing the progress listener callbacks. "
                        + e.getCause().getMessage());
        }
    }

    /**
     * Tests that the user-specified progress listener is properly notified with
     * all the request/response progress event code.
     */
    @Test
    public void testProgressEventNotification_SuccessfulRequest() {
        BatchWriteItemRequest request = generateLargeBatchWriteItemRequest();

        ExceptionReporter listener = ExceptionReporter.wrap(new ProgressListenerWithEventCodeVerification(
                ProgressEventType.CLIENT_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT,
                ProgressEventType.HTTP_RESPONSE_STARTED_EVENT,
                ProgressEventType.HTTP_RESPONSE_COMPLETED_EVENT,
                ProgressEventType.CLIENT_REQUEST_SUCCESS_EVENT));
        request.setGeneralProgressListener(listener);

        dynamo.batchWriteItem(request);
        waitTillListenerCallbacksComplete();
        listener.throwExceptionIfAny();
    }

    @Test
    public void testProgressEventNotification_FailedRequest_NoRetry() {
        // An invalid PutItemRequest that does not have the key attribute value
        PutItemRequest request = PutItemRequest.builder()
                .tableName(BasicTempTable.TEMP_TABLE_NAME)
                .item(ImmutableMapParameter.of("foo", AttributeValue.builder().s("bar").build()))
                .build();

        ExceptionReporter listener = ExceptionReporter.wrap(new ProgressListenerWithEventCodeVerification(
                ProgressEventType.CLIENT_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT,
                ProgressEventType.CLIENT_REQUEST_FAILED_EVENT));
        request.setGeneralProgressListener(listener);

        RetryPolicy retryPolicy = new RetryPolicy((originalRequest, exception, retriesAttempted) -> false,
                                                  PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 0, false);

        DynamoDBClient ddb_NoRetry = DynamoDBClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy(new RetryPolicyAdapter(retryPolicy)).build())
                .build();

        try {
            ddb_NoRetry.putItem(request);
            Assert.fail("Exception is expected since the PutItemRequest is invalid.");
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }

        waitTillListenerCallbacksComplete();
        listener.throwExceptionIfAny();
    }

    @Test
    public void testProgressEventNotification_FailedRequest_WithRetry() {
        // An invalid PutItemRequest that does not have the key attribute value
        PutItemRequest request = PutItemRequest.builder()
                .tableName(BasicTempTable.TEMP_TABLE_NAME)
                .item(ImmutableMapParameter.of("foo", AttributeValue.builder().s("bar").build()))
                .build();

        RetryPolicy retryPolicy = new RetryPolicy((originalRequest, exception, retriesAttempted) -> true,
                                                  PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 2, false);

        DynamoDBClient ddb_OneRetry = DynamoDBClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy(new RetryPolicyAdapter(retryPolicy))
                                                                  .build())
                .build();

        ExceptionReporter listener = ExceptionReporter.wrap(new ProgressListenerWithEventCodeVerification(
                ProgressEventType.CLIENT_REQUEST_STARTED_EVENT,
                // First attempt
                ProgressEventType.HTTP_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT,
                // Second attempt
                ProgressEventType.CLIENT_REQUEST_RETRY_EVENT,
                ProgressEventType.HTTP_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT,
                // Third attempt
                ProgressEventType.CLIENT_REQUEST_RETRY_EVENT,
                ProgressEventType.HTTP_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT,
                ProgressEventType.CLIENT_REQUEST_FAILED_EVENT));
        request.setGeneralProgressListener(listener);


        try {
            ddb_OneRetry.putItem(request);
            Assert.fail("Exception is expected since the PutItemRequest is invalid.");
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }

        waitTillListenerCallbacksComplete();
        listener.throwExceptionIfAny();
    }

    /**
     * Tests that RequestCycleProgressUpdatingListener properly tracks the
     * request/response progress.
     */
    @Test
    public void testRequestCycleProgressReporting() {
        ProgressTracker tracker = new ProgressTracker();
        BatchWriteItemRequest request = generateLargeBatchWriteItemRequest()
                .withGeneralProgressListener(tracker);
        dynamo.batchWriteItem(request);
        Progress progress = tracker.getProgress();
        Assert.assertTrue(progress.getRequestContentLength() > 0);
        Assert.assertEquals((Long) progress.getRequestContentLength(),
                            (Long) progress.getRequestBytesTransferred());
        Assert.assertTrue(progress.getResponseContentLength() > 0);
        Assert.assertEquals((Long) progress.getResponseContentLength(),
                            (Long) progress.getResponseBytesTransferred());
    }

    private static class RandomStringGenerator {

        private static final String characters = "abcdefghijklmnopqrstuvwxyz";
        private static final Random random = new Random();

        public static String nextRandomString(long length) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append(characters.charAt(random.nextInt(characters.length())));
            }
            return sb.toString();
        }
    }
}
