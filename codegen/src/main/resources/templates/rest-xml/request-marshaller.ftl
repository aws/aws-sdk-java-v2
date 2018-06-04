${fileHeader}
package ${transformPackage};

import static software.amazon.awssdk.core.utils.FunctionalUtils.invokeSafely;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
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
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.core.util.Md5Utils;
import software.amazon.awssdk.core.util.XmlWriter;
import software.amazon.awssdk.core.util.SdkHttpUtils;

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

        <#assign httpVerb = (shape.marshaller.verb)!POST/>
        request.setHttpMethod(HttpMethodName.${httpVerb});

        <@MarshalHeaderMembersMacro.content shape shape.variable.variableName/>

        <#list shape.members as member>
            <#if member.map && member.http.location?? && member.http.location == "headers">
            if(${shape.variable.variableName}.${member.variable.variableName}() != null) {
                ${shape.variable.variableName}.${member.variable.variableName}().entrySet().forEach(e -> {
                    if (e.getKey().startsWith("${member.http.marshallLocationName}")) {
                        request.addHeader(e.getKey(), e.getValue());
                    } else {
                        request.addHeader("${member.http.marshallLocationName}" + e.getKey(), e.getValue());
                    }
                });
            }
            </#if>
        </#list>

        <@UriMemberMarshallerMacro.content shape shape.variable.variableName/>
        <@QueryStringMemberMarshallerMacro.content shape shape.variable.variableName/>

        <#if shape.hasPayloadMember>
            <#list shape.members as member>
                <#if (member.http.isStreaming)>
                <#-- Content is set by StreamingRequestMarshaller -->
                <#elseif (member.http.isPayload) && member.variable.variableType = "java.nio.ByteBuffer">
                request.setContent(BinaryUtils.toStream(${shape.variable.variableName}.${member.fluentGetterMethodName}()));
                if (!request.getHeaders().containsKey("Content-Type")) {
                    request.addHeader("Content-Type", "binary/octet-stream");
                }
                <#-- This is for S3 PutBucketPolicy -->
                <#elseif member.http.isPayload && member.variable.variableType = "String">
                ${member.variable.variableType} ${member.variable.variableName} = ${shape.variable.variableName}.${member.fluentGetterMethodName}();
                if(${member.variable.variableName} != null) {
                    byte[] content = ${member.variable.variableName}.getBytes(StandardCharsets.UTF_8);
                    request.setContent(new ByteArrayInputStream(content));
                    request.addHeader("Content-Length", Integer.toString(content.length));
                    if (!request.getHeaders().containsKey("Content-MD5")) {
                        request.addHeader("Content-MD5", Md5Utils.md5AsBase64(content));
                    }
                }
                <#elseif member.http.isPayload>
                try {
                    StringWriter stringWriter = null;
                    ${member.variable.variableType} ${member.variable.variableName} = ${shape.variable.variableName}.${member.fluentGetterMethodName}();
                    if (${member.variable.variableName} != null) {
                        stringWriter = new StringWriter();
                        <#-- xmlNameSpaceUri comes from the payload member reference -->
                        <#if member.xmlNameSpaceUri??>
                        XmlWriter xmlWriter = new XmlWriter(stringWriter, "${member.xmlNameSpaceUri}");
                        <#else>
                        XmlWriter xmlWriter = new XmlWriter(stringWriter);
                        </#if>
                        xmlWriter.startElement("${member.http.marshallLocationName}");
                        <@MemberMarshallerMacro.content customConfig member.c2jShape member.variable.variableName shapes/>
                        xmlWriter.endElement();
                    }

                    if (stringWriter != null) {
                        <#-- S3 requires Content-MD5 for some APIs. This sets it for all APIs -->
                        <#-- TODO @ReviewBeforeRelease this should probably be done in a request handler -->
                        <#if metadata.serviceName == "Amazon S3">
                        if (!request.getHeaders().containsKey("Content-MD5")) {
                            request.addHeader("Content-MD5", Md5Utils.md5AsBase64(stringWriter.getBuffer().toString().getBytes(UTF_8)));
                        }
                        </#if>
                        request.setContent(new StringInputStream(stringWriter.getBuffer().toString()));
                        request.addHeader("Content-Length", Integer.toString(stringWriter.getBuffer().toString().getBytes(UTF_8).length));
                    }
                    if (!request.getHeaders().containsKey("Content-Type")) {
                        request.addHeader("Content-Type", "application/xml");
                    }
                } catch(Throwable t) {
                    throw new SdkClientException("Unable to marshall request to XML: " + t.getMessage(), t);
                }
                <#break>
                </#if>
            </#list>
        <#elseif shape.unboundMembers?has_content>
        try {
            StringWriter stringWriter = new StringWriter();
            <#-- xmlNameSpaceUri comes from the operation input reference -->
            XmlWriter xmlWriter = new XmlWriter(stringWriter, "${shape.marshaller.xmlNameSpaceUri}");

            xmlWriter.startElement("${shape.marshaller.locationName}");
            <@MemberMarshallerMacro.content customConfig shapeName shape.variable.variableName shapes/>
            xmlWriter.endElement();

            request.setContent(new StringInputStream(stringWriter.getBuffer().toString()));
            request.addHeader("Content-Length", Integer.toString(stringWriter.getBuffer().toString().getBytes(UTF_8).length));
            if (!request.getHeaders().containsKey("Content-Type")) {
                request.addHeader("Content-Type", "application/xml");
            }
        } catch(Throwable t) {
            throw new SdkClientException("Unable to marshall request to XML: " + t.getMessage(), t);
        }
        </#if>

        <#-- TODO @ReviewBeforeRelease codegen templates should not have service specific customizations -->
        <#if shapeName == "CreateMultipartUploadRequest">
        request.setContent(new ByteArrayInputStream(new byte[0]));
        request.addHeader("Content-Length", String.valueOf(0));
        </#if>

        return request;
    }

    <@RequiredParameterValidationFunctionMacro.content customConfig shape/>
}