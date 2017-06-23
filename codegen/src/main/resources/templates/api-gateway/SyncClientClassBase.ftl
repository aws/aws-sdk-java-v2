${fileHeader}
<#assign serviceAbbreviation = (metadata.serviceAbbreviation)!metadata.serviceFullName/>
package ${metadata.fullClientPackageName};

<#if metadata.hasApiWithStreamInput>
import java.io.*;
</#if>
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import javax.annotation.Generated;

import org.apache.commons.logging.*;

import software.amazon.awssdk.*;
import software.amazon.awssdk.opensdk.*;
import software.amazon.awssdk.opensdk.model.*;
import software.amazon.awssdk.opensdk.protect.model.transform.*;
import software.amazon.awssdk.auth.*;
import software.amazon.awssdk.handlers.*;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.metrics.*;
import software.amazon.awssdk.metrics.spi.*;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics.Field;
import software.amazon.awssdk.regions.*;
import software.amazon.awssdk.runtime.transform.*;
import software.amazon.awssdk.util.*;
import software.amazon.awssdk.protocol.json.*;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.ClientHandlerParams;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.opensdk.protect.client.SdkClientHandler;
<#if customizationConfig.serviceClientHoldInputStream>
import software.amazon.awssdk.util.ServiceClientHolderInputStream;
</#if>
import ${serviceBaseExceptionFqcn};


import ${metadata.fullModelPackageName}.*;
import ${metadata.fullTransformPackageName}.*;

<#assign documentation = (metadata.documentation)!""/>

/**
 * Client for accessing ${serviceAbbreviation}.  All service calls made
 * using this client are blocking, and will not return until the service call
 * completes.
 * <p>
 * ${documentation}
 */
@ThreadSafe
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
class ${metadata.syncClient} implements ${metadata.syncInterface} {


    private final ClientHandler clientHandler;

    <@AdditionalSyncClientFieldsMacro.content .data_model />

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation} using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @param clientParams Object providing client parameters.
     */
    ${metadata.syncClient}(AwsSyncClientParams clientParams) {
        this.clientHandler = new SdkClientHandler(
            new ClientHandlerParams().withClientParams(clientParams));
    }

<#list operations?values as operationModel>
    <@ClientMethodForOperation.content metadata operationModel />
</#list>

    /**
     * Create the error response handler for the operation.
     * @param errorShapeMetadata Error metadata for the given operation
     * @return Configured error response handler to pass to HTTP layer
     */
    private HttpResponseHandler<SdkBaseException> createErrorResponseHandler(
            JsonErrorShapeMetadata... errorShapeMetadata) {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata()
                                                                  .withErrorShapes(Arrays.asList(
                                                                          errorShapeMetadata)));
    }

    @Override
    public void shutdown() {
        clientHandler.shutdown();
    }

}
