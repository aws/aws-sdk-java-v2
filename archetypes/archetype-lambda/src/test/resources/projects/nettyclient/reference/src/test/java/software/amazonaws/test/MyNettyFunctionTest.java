package software.amazonaws.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MyNettyFunctionTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        MyNettyFunction function = new MyNettyFunction();
        Object result = function.handleRequest("echo", null);
        assertEquals("echo", result);
    }
}
