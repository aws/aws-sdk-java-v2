package software.amazon.eventstream;

import org.junit.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class ToStringTest {
    @Test
    public void headerTypes() throws Exception {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":content-type", HeaderValue.fromString("application/json"));
        headers.put("boolean-true", HeaderValue.fromBoolean(true));
        headers.put("boolean-false", HeaderValue.fromBoolean(false));
        headers.put("byte-value", HeaderValue.fromByte((byte) 4));
        headers.put("short-value", HeaderValue.fromShort((short) 16384));
        headers.put("integer-value", HeaderValue.fromInteger(-1048576));
        headers.put("long-value", HeaderValue.fromLong(850270403920392L));
        headers.put("string-value", HeaderValue.fromString("asdf"));
        headers.put("byte-array-value", HeaderValue.fromByteArray(new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }));
        headers.put("instant-value", HeaderValue.fromTimestamp(Instant.ofEpochMilli(1511312536153L)));
        headers.put("uuid-value", HeaderValue.fromUuid(new UUID(89012350712350912L, 9182739072970134201L)));

        Message message = new Message(
            headers,
            "{\"foo\": \"bar\"}".getBytes(UTF_8)
        );

        String expected =
            ":content-type: \"application/json\"\n" +
            "boolean-true: true\n" +
            "boolean-false: false\n" +
            "byte-value: 4\n" +
            "short-value: 16384\n" +
            "integer-value: -1048576\n" +
            "long-value: 850270403920392\n" +
            "string-value: \"asdf\"\n" +
            "byte-array-value: AQIDBAUGBwgJCgsM\n" +
            "instant-value: 2017-11-22T01:02:16.153Z\n" +
            "uuid-value: 013c3c42-e8d5-08c0-7f6f-a488dd7c12b9\n" +
            "\n" +
            "{\"foo\": \"bar\"}\n";
        System.out.println(message);
        assertEquals(expected, message.toString());
    }

    @Test
    public void controlMessages() throws Exception {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":content-type", HeaderValue.fromString("application/json"));

        Message message = new Message(headers, "{\"foo\": \"bar\"}".getBytes(UTF_8));

        String expected =
            ":content-type: \"application/json\"\n" +
            "\n" +
            "{\"foo\": \"bar\"}\n";
        assertEquals(expected, message.toString());
    }

    @Test
    public void binaryPayload() throws Exception {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":content-type", HeaderValue.fromString("application/octet-stream"));

        Message message = new Message(headers, new byte[]{ 23, 12, (byte) 129, 44, 89, 90 });

        String expected =
            ":content-type: \"application/octet-stream\"\n" +
            "\n" +
            "FwyBLFla\n";
        assertEquals(expected, message.toString());
    }
}
