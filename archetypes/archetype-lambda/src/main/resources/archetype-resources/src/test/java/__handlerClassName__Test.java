#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ${handlerClassName}Test {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        ${handlerClassName} function = new ${handlerClassName}();
        Object result = function.handleRequest("echo", null);
        assertEquals("echo", result);
    }
}
