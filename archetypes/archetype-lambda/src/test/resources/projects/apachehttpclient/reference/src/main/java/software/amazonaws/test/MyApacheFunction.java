package software.amazonaws.test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class MyApacheFunction implements RequestHandler<Map<String, String>, String> {
    private final DynamoDbClient dynamoDbClient;

    public MyApacheFunction() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.
        dynamoDbClient = DependencyFactory.dynamoDbClient();
        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
    }

    @Override
    public String handleRequest(final Map<String, String> input, final Context context) {
        LambdaLogger lambdaLogger = context.getLogger();
        lambdaLogger.log("Start to handle request");
        // TODO: invoking the api call using dynamoDbClient.
        return "";
    }
}
