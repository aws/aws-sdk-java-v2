package software.amazonaws.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MyCrtFunctionTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        MyCrtFunction function = new MyCrtFunction();
        Object result = function.handleRequest("echo", null);
        assertEquals("echo", result);
    }
}
