package waiter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.jmespath.InvalidTypeException;
import software.amazon.awssdk.jmespath.JmesPathContainsFunction;

/**
 * Created by meghbyar on 8/3/16.
 */
public class JmesPathContainsFunctionTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void jmesPathContainsFunction_Evaluate_Array_ReturnsTrueIfArrayContainsElement() throws IOException {
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.readTree("[2, 3, 4]"));
        args.add(objectMapper.readTree("2"));
        Assert.assertEquals(BooleanNode.TRUE, new JmesPathContainsFunction().evaluate(args));
    }

    @Test
    public void jmesPathContainsFunction_Evaluate_Array_ReturnsFalse() throws IOException {
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.readTree("[2, 3, 4]"));
        args.add(objectMapper.readTree("5"));
        Assert.assertEquals(BooleanNode.FALSE, new JmesPathContainsFunction().evaluate(args));
    }

    @Test
    public void jmesPathContainsFunction_Evaluate_String_ReturnsTrueIfStringContainsElement() throws IOException{
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.readTree("\"HelloThere\""));
        args.add(objectMapper.readTree("\"Th\""));
        Assert.assertEquals(BooleanNode.TRUE, new JmesPathContainsFunction().evaluate(args));
    }

    @Test
    public void jmesPathContainsFunction_Evaluate_String_ReturnsFalse() throws IOException{
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.readTree("\"HelloThere\""));
        args.add(objectMapper.readTree("\"th\""));
        Assert.assertEquals(BooleanNode.FALSE, new JmesPathContainsFunction().evaluate(args));
    }

    @Test(expected = InvalidTypeException.class)
    public void jmesPathContainsFunction_Evaluate_NotStringOrArray_ThrowsInvalidTypeException() throws IOException{
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.readTree("234"));
        args.add(objectMapper.readTree("2"));
        Assert.assertEquals(BooleanNode.TRUE, new JmesPathContainsFunction().evaluate(args));
    }

}
