${fileHeader}
package ${transformPackage};

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.http.HttpMethodName;
import ${metadata.fullModelPackageName}.*;
import software.amazon.awssdk.runtime.transform.Marshaller;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.util.IdempotentUtils;


/**
 * ${shapeName} Marshaller
 */

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shapeName}Marshaller implements Marshaller<Request<${shapeName}>, ${shapeName}> {

<#assign shape = shapes[shapeName]/>
    public Request<${shapeName}> marshall(${shape.variable.variableType} ${shape.variable.variableName}) {

        if (${shape.variable.variableName} == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
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
