package software.amazon.awssdk.services.sqs.buffered;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.SQSAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public class ReceiveQueueBufferTest {

    private static final int VISIBILITY_TIMEOUT_SECONDS = 1;

    /**
     * The tests below are very coupled to this constant. Do not change it unless you plan on
     * changing the test
     */
    private static final int NUMBER_OF_BATCHES = 2;

    private static final Map<String, String> QUEUE_ATTRIBUTES = new HashMap<String, String>();

    static {
        QUEUE_ATTRIBUTES.put(QueueAttributeName.VisibilityTimeout.toString(),
                String.valueOf(VISIBILITY_TIMEOUT_SECONDS));
    }

    private SQSAsyncClient mockSqs;
    private ReceiveQueueBuffer buffer;

    @Before
    public void setup() {
        mockSqs = createMock(SQSAsyncClient.class);
        expectGetQueueAttributes(QUEUE_ATTRIBUTES);
        buffer = new ReceiveQueueBuffer(mockSqs, MoreExecutors.newDirectExecutorService(), getQueueBufferConfig(),
                "some-queue");
    }

    //@formatter:off
    /**
     * Tests edge case where calls to receive message can miss valid messages due to expiration. The
     * scenario is as follows:
     * <ol>
     * <li>
     * No messages in the buffer
     * </li>
     * <li>
     * We call receiveMessage which queues up a task to fetch messages from SQS. First call to SQS
     * finds two messages that are fulfilled immediately. When it returns another call is queued up
     * that returns a single message from SQS.
     * </li>
     * <li>
     * Wait for enough time to make the one message returned from SQS expire.
     * </li>
     * <li>
     * Another call to SQS is made which returns 0 messages. At this point the first batch in the
     * buffer has one message which is expired and the second batch has zero messages but is non
     * expired.
     * </li>
     * <li>
     * When we make a second call to receive messages it should prune the expired batch of 1
     * message, and also prune any empty batches afterwards so new batches can be fetched from SQS.
     * This should finally result in one last call to SQS which returns the 1 message and is
     * fulfilled immediately.
     * </ol>
     */
    //@formatter:on
    @Test
    public void receiveMessages_ExpiredMessageAtHeadOfBuffer_PurgesEmptyMessagesFromBuffer()
            throws InterruptedException, ExecutionException {
        // These calls to SQS are triggered by the first receiveMessage call. The first one with is
        // fulfilled immediately, allowing two more calls to be made to meet the batch count
        expectReceiveMessages(Message.builder().build(), Message.builder().build()); // First assertion
        expectReceiveMessagesWithSleep(VISIBILITY_TIMEOUT_SECONDS + 1, Message.builder().build());
        expectReceiveMessages();

        // Since the expired message and the empty message following it are purged this will queue
        // up another call to SQS returning one message that will be fulfilled immediately. Two more
        // calls will be made to again meet the batch count (we don't really care about the last two
        // calls, just the fact they are made)
        expectReceiveMessages(Message.builder().build()); // Second assertion
        expectReceiveMessages();
        expectReceiveMessages();

        replay(mockSqs);
        assertEquals(2, receiveMessageWithSize());
        assertEquals(1, receiveMessageWithSize());
        verify(mockSqs);
    }

    private void expectGetQueueAttributes(Map<String, String> queueAttributes) {
        expect(mockSqs.getQueueAttributes(isA(GetQueueAttributesRequest.class))).andStubReturn(
                CompletableFuture.completedFuture(GetQueueAttributesResponse.builder().attributes(queueAttributes).build()));
    }

    private void expectReceiveMessages(Message... messages) {
        expectReceiveMessagesWithSleep(0, asArray(messages));
    }

    /**
     * Helper to expect a call to mockSqs.receiveMessage with an optional sleep time and a list of
     * messages to return to the call
     * 
     * @param sleepSeconds
     *            Optional sleep time. If positive will sleep for that many seconds before returning
     *            the messages
     * @param messages
     *            Messages to return
     */
    private void expectReceiveMessagesWithSleep(final int sleepSeconds, final Message... messages) {
        expect(mockSqs.receiveMessage(isA(ReceiveMessageRequest.class))).andAnswer(() -> {
            if (sleepSeconds > 0) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(sleepSeconds));
            }
            return CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(asArray(messages)).build());
        });
    }

    /**
     * Hack to allow passing vararg parameters to other methods that take varargs
     */
    private Message[] asArray(Message[] messages) {
        return messages;
    }

    private QueueBufferConfig getQueueBufferConfig() {
        QueueBufferConfig queueBufferConfig = new QueueBufferConfig();
        queueBufferConfig.withMaxDoneReceiveBatches(NUMBER_OF_BATCHES).withMaxInflightOutboundBatches(
                NUMBER_OF_BATCHES - 1);
        return queueBufferConfig;
    }

    /**
     * Helper method to call receiveMessageAsync on the buffer and return the number of messages in
     * the result
     */
    private int receiveMessageWithSize() throws InterruptedException, ExecutionException {
        return buffer.receiveMessageAsync(ReceiveMessageRequest.builder().build(), null).get().messages().size();
    }
}
