package software.amazonaws.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MyWafRegionalFunctionTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        MyWafRegionalFunction function = new MyWafRegionalFunction();
        Object result = function.handleRequest("echo", null);
        assertEquals("echo", result);
    }
}
