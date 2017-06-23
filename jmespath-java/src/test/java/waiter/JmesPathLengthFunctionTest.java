package waiter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.jmespath.InvalidTypeException;
import software.amazon.awssdk.jmespath.JmesPathLengthFunction;

/**
 * Created by meghbyar on 8/3/16.
 */
public class JmesPathLengthFunctionTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void jmesPathContainsFunction_Evaluate_Array_ReturnsLengthOfArray() throws IOException {
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.readTree("[2, 3, 4]"));
        Assert.assertEquals(new IntNode(3), new JmesPathLengthFunction().evaluate(args));
    }

    @Test
    public void jmesPathContainsFunction_Evaluate_String_ReturnsLengthOfString() throws IOException {
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.readTree("\"Sample\""));
        Assert.assertEquals(new IntNode(6), new JmesPathLengthFunction().evaluate(args));
    }

    @Test
    public void jmesPathContainsFunction_Evaluate_Object_ReturnsLengthOfObject() throws IOException {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one", 1);
        map.put("two", 2);
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.valueToTree(map));
        Assert.assertEquals(new IntNode(2), new JmesPathLengthFunction().evaluate(args));
    }

    @Test(expected = InvalidTypeException.class)
    public void jmesPathContainsFunction_Evaluate_NotStringOrArray_ThrowsInvalidTypeException() throws IOException {
        List<JsonNode> args = new ArrayList<>();
        args.add(objectMapper.readTree("234"));
        Assert.assertEquals(new IntNode(3), new JmesPathLengthFunction().evaluate(args));
    }
}
