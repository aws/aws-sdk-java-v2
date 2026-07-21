package software.amazon.awssdk.mapper.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ClientConfiguration;
import software.amazon.awssdk.mapper.dynamodb.test.resources.tables.BasicTempTable;
import software.amazon.awssdk.mapper.dynamodb.test.util.DynamoDBTestBase;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener.ExceptionReporter;
import com.amazonaws.event.ProgressTracker;
import com.amazonaws.event.SDKProgressPublisher;
import com.amazonaws.event.request.Progress;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.retry.RetryPolicy.RetryCondition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import software.amazon.awssdk.mapper.dynamodb.test.resources.RequiredResources;
import software.amazon.awssdk.mapper.dynamodb.test.resources.RequiredResources.RequiredResource;
import software.amazon.awssdk.mapper.dynamodb.test.resources.RequiredResources.ResourceCreationPolicy;
import software.amazon.awssdk.mapper.dynamodb.test.resources.RequiredResources.ResourceRetentionPolicy;
import software.amazon.awssdk.mapper.dynamodb.test.resources.ResourceCentricBlockJUnit4ClassRunner;
import software.amazon.awssdk.mapper.dynamodb.test.util.ProgressListenerWithEventCodeVerification;
import com.amazonaws.util.ImmutableMapParameter;

public class RequestProgressTest extends LocalDynamoDBTestBase {
    private static final long KB = 1024;
    private static AmazonDynamoDB dynamo;

    @BeforeClass
    public static void setup() {
        dynamo = client();
        dynamo.createTable(BasicTempTable.getCreateTableRequest());
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
        PutItemRequest request = new PutItemRequest(
                BasicTempTable.TEMP_TABLE_NAME,
                ImmutableMapParameter.of("foo", new AttributeValue("bar")));

        ExceptionReporter listener = ExceptionReporter.wrap(new ProgressListenerWithEventCodeVerification(
                ProgressEventType.CLIENT_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT,
                ProgressEventType.CLIENT_REQUEST_FAILED_EVENT));
        request.setGeneralProgressListener(listener);

        ClientConfiguration config = new ClientConfiguration().withRetryPolicy(new RetryPolicy(new RetryCondition() {

            @Override
            public boolean shouldRetry(AmazonWebServiceRequest originalRequest,
                    AmazonClientException exception, int retriesAttempted) {
                return false;
            }

        }, PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 0, false));

        AmazonDynamoDB ddb_NoRetry = client(config);
        try {
            ddb_NoRetry.putItem(request);
            Assert.fail("Exception is expected since the PutItemRequest is invalid.");
        } catch (AmazonServiceException expected) {}

        waitTillListenerCallbacksComplete();
        listener.throwExceptionIfAny();
    }

    @Test
    public void testProgressEventNotification_FailedRequest_WithRetry() {
        // An invalid PutItemRequest that does not have the key attribute value
        PutItemRequest request = new PutItemRequest(
                BasicTempTable.TEMP_TABLE_NAME,
                ImmutableMapParameter.of("foo", new AttributeValue("bar")));

        // ClientConfiguration that specifies a maximum of two retries
        ClientConfiguration config = new ClientConfiguration().withRetryPolicy(new RetryPolicy(new RetryCondition() {

            @Override
            public boolean shouldRetry(AmazonWebServiceRequest originalRequest,
                    AmazonClientException exception, int retriesAttempted) {
                return true;
            }

        }, PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 2, false));

        AmazonDynamoDB ddb_OneRetry = client(config);

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
        } catch (AmazonServiceException expected) {}

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
        Assert.assertEquals((Long)progress.getRequestContentLength(),
                            (Long)progress.getRequestBytesTransferred());
        Assert.assertTrue(progress.getResponseContentLength() > 0);
        Assert.assertEquals((Long)progress.getResponseContentLength(),
                            (Long)progress.getResponseBytesTransferred());
    }

    private static BatchWriteItemRequest generateLargeBatchWriteItemRequest() {
        List<WriteRequest> writes = new LinkedList<WriteRequest>();
        for (int i = 0; i < 25; i++) {
            writes.add(new WriteRequest(new PutRequest(
                    ImmutableMapParameter.of(
                            BasicTempTable.HASH_KEY_NAME, new AttributeValue(Integer.toString(i)),
                            "large-random-string", new AttributeValue(RandomStringGenerator.nextRandomString(40 * KB))))));
        }
        return new BatchWriteItemRequest(
                Collections.singletonMap(BasicTempTable.TEMP_TABLE_NAME, writes));
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

    private static void waitTillListenerCallbacksComplete() {
        try {
            SDKProgressPublisher.waitTillCompletion();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted when waiting for the progress listener callbacks to return. "
                    + e.getMessage());
        } catch (ExecutionException e) {
            Assert.fail("Error when executing the progress listner callbacks. "
                    + e.getCause().getMessage());
        }
    }
}
