${fileHeader}
package ${transformPackage};

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.http.HttpMethodName;
import ${metadata.fullModelPackageName}.*;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.core.util.IdempotentUtils;


/**
 * ${shapeName} Marshaller
 */

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shapeName}Marshaller implements Marshaller<Request<${shapeName}>, ${shapeName}> {

<#assign shape = shapes[shapeName]/>
    public Request<${shapeName}> marshall(${shape.variable.variableType} ${shape.variable.variableName}) {

        if (${shape.variable.variableName} == null) {
            throw SdkClientException.builder().message("Invalid argument passed to marshall(...)").build();
        }

       <@RequiredParameterValidationInvocationMacro.content customConfig shape/>

       <#assign serviceNameForRequest = customConfig.customServiceNameForRequest!metadata.syncInterface />

        Request<${shape.shapeName}> request = new DefaultRequest<${shape.shapeName}>(${shape.variable.variableName}, "${serviceNameForRequest}");
        request.addParameter("Action", "${shape.marshaller.action}");
        <#if metadata.apiVersion?has_content>request.addParameter("Version", "${metadata.apiVersion}");</#if>
        <#if shape.marshaller.verb?has_content>request.setHttpMethod(HttpMethodName.${shape.marshaller.verb});</#if>

        <@MemberMarshallerMacro.content customConfig shapeName shape.variable.variableName shapes ""/>

        return request;
    }

    <@RequiredParameterValidationFunctionMacro.content customConfig shape/>
}
