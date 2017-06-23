package waiter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.jmespath.JmesPathAndExpression;
import software.amazon.awssdk.jmespath.JmesPathContainsFunction;
import software.amazon.awssdk.jmespath.JmesPathEvaluationVisitor;
import software.amazon.awssdk.jmespath.JmesPathExpression;
import software.amazon.awssdk.jmespath.JmesPathField;
import software.amazon.awssdk.jmespath.JmesPathFilter;
import software.amazon.awssdk.jmespath.JmesPathFlatten;
import software.amazon.awssdk.jmespath.JmesPathIdentity;
import software.amazon.awssdk.jmespath.JmesPathLengthFunction;
import software.amazon.awssdk.jmespath.JmesPathLiteral;
import software.amazon.awssdk.jmespath.JmesPathMultiSelectList;
import software.amazon.awssdk.jmespath.JmesPathNotExpression;
import software.amazon.awssdk.jmespath.JmesPathProjection;
import software.amazon.awssdk.jmespath.JmesPathSubExpression;
import software.amazon.awssdk.jmespath.JmesPathValueProjection;
import software.amazon.awssdk.jmespath.OpEquals;
import software.amazon.awssdk.jmespath.OpGreaterThan;
import software.amazon.awssdk.jmespath.OpGreaterThanOrEqualTo;

/**
 * Created by meghbyar on 7/13/16.
 */
//JmesPath expression integration test
//Tests end to end JmesPath expression evaluation for those defined in Waiter specification
//Java ASTs are got as output of JmesPathCodeGenVisitor
public class JmesPathTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    //Tests the expression table.tableStatus
    @Test
    public void jmesPathExprOne() throws IOException{
        JmesPathExpression javaAst = new JmesPathSubExpression(new JmesPathField("table"), new JmesPathField("tableStatus"));
        JsonNode queryNode = objectMapper.readTree("{\"table\" : {\"tableStatus\" : \"ACTIVE\"}}");
        Assert.assertEquals("ACTIVE", javaAst.accept(new JmesPathEvaluationVisitor(), queryNode).asText());
    }

    //Tests the expression length(Reservations[].KeyName) > `0`
    @Test
    public void jmesPathExprTwo() throws IOException{
        JmesPathExpression javaAst = new OpGreaterThan(new JmesPathLengthFunction( new JmesPathProjection(new JmesPathFlatten(new JmesPathField("Reservations")), new JmesPathField("KeyName")) ), new JmesPathLiteral("0"));
        JsonNode queryNode = objectMapper.readTree("{\"reservations\": [{\"keyName\" : \"foo\"}, {\"keyName\" : \"bar\"}] }");
        Assert.assertEquals(BooleanNode.TRUE, javaAst.accept(new JmesPathEvaluationVisitor(), queryNode));
    }

    //Tests the expression Reservations[].Instances[].state
    @Test
    public void jmesPathExprThree() throws IOException{
        JsonNode jsonNode = objectMapper.readTree("[\"running\",\"stopped\",\"terminated\",\"runnning\"]");
        JmesPathExpression javaAst = new JmesPathProjection(new JmesPathFlatten(new JmesPathProjection(new JmesPathFlatten(new JmesPathField("Reservations")), new JmesPathField("Instances"))), new JmesPathField("state"));
        JsonNode queryNode = objectMapper.readTree("{\r\n  \"reservations\": [\r\n    {\r\n      \"instances\": [\r\n        {\"state\": \"running\"},\r\n        {\"state\": \"stopped\"}\r\n      ]\r\n    },\r\n    {\r\n      \"instances\": [\r\n        {\"state\": \"terminated\"},\r\n        {\"state\": \"runnning\"}\r\n      ]\r\n    }\r\n  ]\r\n}");
        Assert.assertEquals(jsonNode, (JsonNode)javaAst.accept(new JmesPathEvaluationVisitor(), queryNode));
    }

    //Tests the expression length(services[?!(length(deployments) == `1` && runningCount == desiredCount)]) == `0`
    @Test
    public void jmesPathExprFour() throws IOException{
        JmesPathExpression javaAst = new OpEquals(new JmesPathLengthFunction( new JmesPathFilter( new JmesPathField( "services"), new JmesPathIdentity(), new JmesPathNotExpression( new JmesPathAndExpression( new OpEquals(new JmesPathLengthFunction( new JmesPathField( "deployments") ), new JmesPathLiteral("1")), new OpEquals(new JmesPathField( "runningCount"), new JmesPathField( "desiredCount")) ) )) ), new JmesPathLiteral("0"));
        JsonNode queryNode = objectMapper.readTree("{ \"services\" : [{ \"deployments\" : { \"foo\" : \"bar\"}, \"runningCount\" : 2, \"desiredCount\" : 2}]}");
        Assert.assertEquals("", BooleanNode.TRUE, javaAst.accept(new JmesPathEvaluationVisitor(), queryNode));
    }

    //Tests the expression contains(AutoScalingGroups[][length(Instances[?LifeCycleState==`InService`]) >= MinSize][], `false`)
    @Test
    public void jmesPathExprFive() throws IOException{
        JmesPathExpression javaAst = new JmesPathContainsFunction( new JmesPathProjection( new JmesPathFlatten( new JmesPathProjection( new JmesPathFlatten( new JmesPathField( "AutoScalingGroups")), new JmesPathMultiSelectList( new OpGreaterThanOrEqualTo(new JmesPathLengthFunction( new JmesPathFilter( new JmesPathField( "Instances"), new JmesPathIdentity(), new OpEquals(new JmesPathField( "LifeCycleState"), new JmesPathLiteral("\"InService\""))) ), new JmesPathField( "MinSize")) ))), new JmesPathIdentity()), new JmesPathLiteral("false") );
        JsonNode queryNode = objectMapper.readTree("{\r\n    \"autoScalingGroups\": [\r\n        {\r\n            \"minSize\": 1,\r\n            \"instances\": [\r\n                {\r\n                    \"lifeCycleState\": \"InService\"\r\n                    \r\n                },\r\n                {\r\n                    \"lifeCycleState\": \"InService\"\r\n                    \r\n                }\r\n            ]\r\n        },\r\n        \r\n        {\r\n            \"minSize\": 3,\r\n            \"instances\": [\r\n                {\r\n                    \"lifeCycleState\": \"InService\"\r\n                    \r\n                },\r\n                {\r\n                    \"lifeCycleState\": \"InService\"\r\n                   \r\n                }\r\n            ]\r\n        }\r\n    ]\r\n}");
        Assert.assertEquals(BooleanNode.TRUE, javaAst.accept(new JmesPathEvaluationVisitor(), queryNode));
    }

    //Tests the expression foo.bar.baz
    @Test
    public void jmesPathExprSix() throws IOException{
        JmesPathExpression javaAst = new JmesPathSubExpression( new JmesPathField( "foo"), new JmesPathField( "bar"), new JmesPathField( "baz") );
        JsonNode queryNode = objectMapper.readTree("{\"foo\": {\"bar\": {\"baz\" : \"result\"}}}");
        Assert.assertEquals(new TextNode("result"), javaAst.accept(new JmesPathEvaluationVisitor(), queryNode));
    }


    //Tests the expression ops.*.numArgs
    @Test
    public void jmesPathExprSeven() throws IOException{
        JsonNode jsonNode = objectMapper.readTree("[2, 3]");
        JmesPathExpression javaAst = new JmesPathValueProjection( new JmesPathField( "ops"), new JmesPathField( "numArgs"));
        JsonNode queryNode = objectMapper.readTree("{\r\n  \"ops\": {\r\n    \"functionA\": {\"numArgs\": 2},\r\n    \"functionB\": {\"numArgs\": 3},\r\n    \"functionC\": {\"variadic\": true}\r\n  }\r\n}");
        Assert.assertEquals(jsonNode, javaAst.accept(new JmesPathEvaluationVisitor(), queryNode));
    }

}
