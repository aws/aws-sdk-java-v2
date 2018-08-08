package software.amazon.eventstream;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TestUtils {
    private final Random rand;
    private final int maxHeaders;
    private final int maxHeaderSize;
    private final int maxPayloadSize;

    public TestUtils(long seed) {
        this.rand = new Random(seed);
        this.maxHeaders = 20;
        this.maxHeaderSize = 64;
        this.maxPayloadSize = 4096;
    }

    public Message randomMessage() {
        return new Message(randomHeaders(), randomPayload());
    }

    public Message randomMessage(int payloadSize) {
        return new Message(randomHeaders(), randomPayload(payloadSize));
    }

    private Map<String, HeaderValue> randomHeaders() {
        Map<String, HeaderValue> headers = new HashMap<>();
        int numHeaders = rand.nextInt(maxHeaders + 1);
        for (int i = 0; i < numHeaders; i++) {
            headers.put("asdf" + rand.nextInt(), randomHeaderValue());
        }
        return headers;
    }

    private HeaderValue randomHeaderValue() {
        switch (rand.nextInt(7)) {
            case 0:
                return HeaderValue.fromInteger(rand.nextInt(Integer.MAX_VALUE));
            case 1:
                int bytes = rand.nextInt(maxHeaderSize + 1) + 1;
                byte[] buf = new byte[bytes];
                rand.nextBytes(buf);
                return HeaderValue.fromByteArray(buf);
            case 2:
                return HeaderValue.fromString("asdf");
            case 3:
                return HeaderValue.fromBoolean(true);
            case 4:
                return HeaderValue.fromBoolean(false);
            case 5:
                return HeaderValue.fromTimestamp(Instant.ofEpochMilli(rand.nextLong()));
            case 6:
                return HeaderValue.fromUuid(new UUID(rand.nextLong(), rand.nextLong()));
            default:
                throw new IllegalStateException();
        }
    }

    private byte[] randomPayload() {
        return randomPayload(rand.nextInt(maxPayloadSize + 1));
    }

    private byte[] randomPayload(int payloadSize) {
        byte[] ret = new byte[payloadSize];
        rand.nextBytes(ret);
        return ret;
    }
}
