package software.amazonaws.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MyApacheFunctionTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        MyApacheFunction function = new MyApacheFunction();
        Object result = function.handleRequest("echo", null);
        assertEquals("echo", result);
    }
}
