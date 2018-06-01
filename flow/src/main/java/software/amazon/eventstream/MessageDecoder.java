package software.amazon.eventstream;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * A simple decoder that accumulates chunks of bytes and emits eventstream
 * messages. Instances of this class are not thread-safe.
 */
public final class MessageDecoder {

    /**
     * Initial buffer size is 2MB. Will grow as needed to accommodate larger messages.
     */
    private static final int INITIAL_BUFFER_SIZE = 2048 * 1024;

    private final Consumer<Message> messageConsumer;
    private ByteBuffer buf;
    private Prelude currentPrelude;

    public MessageDecoder(Consumer<Message> messageConsumer) {
        this(messageConsumer, INITIAL_BUFFER_SIZE);
    }

    /**
     * To be used by tests only.
     */
    MessageDecoder(Consumer<Message> messageConsumer, int initialBufferSize) {
        this.messageConsumer = messageConsumer;
        this.buf = ByteBuffer.allocate(initialBufferSize);
    }

    public void feed(byte[] bytes) {
        int bytesConsumed = 0;
        while (bytesConsumed < bytes.length) {
            ByteBuffer readView = updateReadView();
            if (currentPrelude == null) {
                // Put only 15 bytes into buffer and compute prelude.
                int numBytesToWrite = Math.min(15 - readView.remaining(),
                                               bytes.length - bytesConsumed);

                buf.put(bytes, bytesConsumed, numBytesToWrite);
                bytesConsumed += numBytesToWrite;
                readView = updateReadView();

                // Have enough data to decode the prelude
                if (readView.remaining() >= 15) {
                    currentPrelude = Prelude.decode(readView.duplicate());
                    if (buf.capacity() < currentPrelude.getTotalLength()) {
                        // Don't have enough capacity to hold this message, grow the buffer
                        buf = ByteBuffer.allocate(currentPrelude.getTotalLength());
                        buf.put(readView);
                        readView = updateReadView();
                    }
                }
            }
            // We might not have received enough data to decode the prelude so check for null again
            if (currentPrelude != null) {
                // Only write up to what we need to decode the next message
                int numBytesToWrite = Math.min(currentPrelude.getTotalLength() - readView.remaining(),
                                               bytes.length - bytesConsumed);

                buf.put(bytes, bytesConsumed, numBytesToWrite);
                bytesConsumed += numBytesToWrite;
                readView = updateReadView();

                // If we have enough data to decode the message do so and reset the buffer for the next message
                if (readView.remaining() >= currentPrelude.getTotalLength()) {
                    messageConsumer.accept(Message.decode(currentPrelude, readView));
                    buf.clear();
                    currentPrelude = null;
                }
            }
        }
    }

    private ByteBuffer updateReadView() {
        return (ByteBuffer) buf.duplicate().flip();
    }

    /**
     * To be used by tests only.
     */
    int currentBufferSize() {
        return buf.capacity();
    }
}
