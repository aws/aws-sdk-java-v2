<#assign shapes = shapes/>
<#assign metadata = metadata/>
<#assign shapeName = shapeName/>
<#assign customConfig = customConfig/>
<#assign shape = shapes[shapeName]/>
<#assign httpVerb = (shape.marshaller.verb)!POST/>
<#assign serviceNameForRequest = customConfig.customServiceNameForRequest!metadata.syncInterface />

${fileHeader}
package ${metadata.fullRequestTransformPackageName};

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Generated;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.http.HttpMethodName;
import ${metadata.fullModelPackageName}.*;
import ${metadata.fullTransformPackageName}.*;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.core.protocol.*;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * ${shapeName} Marshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
@SdkInternalApi
public class ${className} implements Marshaller<Request<${shapeName}>, ${shapeName}> {

    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder()
        .protocolMetadata(Protocol.${protocolEnum})
        .requestUri("${shape.marshaller.requestUri}")
        .httpMethodName(HttpMethodName.${httpVerb})
        .hasExplicitPayloadMember(${shape.hasPayloadMember?c})
        .hasPayloadMembers(${shape.hasPayloadMembers()?c})
        <#if shape.marshaller.target??>
        .operationIdentifier("${shape.marshaller.target}")
        .serviceName("${serviceNameForRequest}")
        </#if>
        .build();

    private final ${metadata.protocolFactory} protocolFactory;

    public ${className}(${metadata.protocolFactory} protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public Request<${shapeName}> marshall(${shape.variable.variableType} ${shape.variable.variableName}) {

        if (${shape.variable.variableName} == null) {
            throw SdkClientException.builder().message("Invalid argument passed to marshall(...)").build();
        }

        <@RequiredParameterValidationInvocationMacro.content customConfig shape/>

        try {
            final ProtocolRequestMarshaller<${shapeName}> protocolMarshaller =
            protocolFactory.createProtocolMarshaller(SDK_OPERATION_BINDING, ${shape.variable.variableName});

            protocolMarshaller.startMarshalling();
            ${shapeName}ModelMarshaller.getInstance().marshall(${shape.variable.variableName}, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch(Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).throwable(e).build();
        }
    }

    <@RequiredParameterValidationFunctionMacro.content customConfig shape/>
}

