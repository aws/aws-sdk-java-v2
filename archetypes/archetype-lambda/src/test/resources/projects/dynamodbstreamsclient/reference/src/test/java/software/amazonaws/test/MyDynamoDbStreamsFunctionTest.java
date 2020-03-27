package software.amazonaws.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MyDynamoDbStreamsFunctionTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        MyDynamoDbStreamsFunction function = new MyDynamoDbStreamsFunction();
        Object result = function.handleRequest("echo", null);
        assertEquals("echo", result);
    }
}
