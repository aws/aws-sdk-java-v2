package software.amazon.eventstream;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static java.lang.Math.toIntExact;

/**
 * A simple decoder that accumulates chunks of bytes and emits eventstream
 * messages. Instances of this class are not thread-safe.
 */
public final class MessageDecoder {
    private final Consumer<Message> messageConsumer;
    private ByteBuffer buf;

    public MessageDecoder(Consumer<Message> messageConsumer) {
        this.messageConsumer = messageConsumer;
        this.buf = ByteBuffer.allocate(128 * 1024);
    }

    public void feed(byte[] bytes) {
        feed(bytes, 0, bytes.length);
    }

    public void feed(byte[] bytes, int offset, int length) {
        buf.put(bytes, offset, length);
        ByteBuffer readView = (ByteBuffer) buf.duplicate().flip();
        int bytesConsumed = 0;
        while (readView.remaining() >= 15) {
            int totalMessageLength = toIntExact(Prelude.decode(readView.duplicate()).getTotalLength());

            if (readView.remaining() >= totalMessageLength) {
                Message decoded = Message.decode(readView);
                messageConsumer.accept(decoded);
                bytesConsumed += totalMessageLength;
            } else {
                break;
            }
        }

        if (bytesConsumed > 0) {
            buf.flip();
            buf.position(buf.position() + bytesConsumed);
            buf.compact();
        }
    }
}
