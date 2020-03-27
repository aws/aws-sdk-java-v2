#parse ( "global.vm")
package ${package};

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.${servicePackage}.${serviceClientClassName};

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class ${handlerClassName} implements RequestHandler<Object, Object> {
    private final ${serviceClientClassName} ${serviceClientVariable}Client;

    public ${handlerClassName}() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.
        ${serviceClientVariable}Client = DependencyFactory.${serviceClientVariable}Client();
        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
    }

    @Override
    public Object handleRequest(final Object input, final Context context) {
        // TODO: invoking the api call using ${serviceClientVariable}Client.
        return input;
    }
}
