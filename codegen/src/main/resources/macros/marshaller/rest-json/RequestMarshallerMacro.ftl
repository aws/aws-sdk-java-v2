<#macro content dataModel>
<#local shapes = dataModel.shapes/>
<#local metadata = dataModel.metadata/>
<#local shapeName = dataModel.shapeName/>
<#local customConfig = dataModel.customConfig/>
<#local contentType = (metadata.contentType)!""/>

${dataModel.fileHeader}
package ${metadata.fullRequestTransformPackageName};

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.awssdk.core.util.StringConversion.COMMA_SEPARATOR;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Generated;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.http.HttpMethodName;
import ${metadata.fullModelPackageName}.*;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.core.util.StringConversion;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.core.util.SdkHttpUtils;
import software.amazon.awssdk.core.protocol.json.*;

/**
 * ${shapeName} Marshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shapeName}Marshaller implements Marshaller<Request<${shapeName}>, ${shapeName}> {

    private final SdkJsonMarshallerFactory protocolFactory;

    public ${shapeName}Marshaller(SdkJsonMarshallerFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    <#local shape = shapes[shapeName]/>
    public Request<${shapeName}> marshall(${shape.variable.variableType} ${shape.variable.variableName}) {

        if (${shape.variable.variableName} == null) {
            throw SdkClientException.builder().message("Invalid argument passed to marshall(...)").build();
        }

        <@RequiredParameterValidationInvocationMacro.content dataModel.customConfig shape/>

       <#assign serviceNameForRequest = customConfig.customServiceNameForRequest!metadata.syncInterface />

        <@DefaultRequestCreation.content shape serviceNameForRequest/>

        <#local httpVerb = (shape.marshaller.verb)!POST/>
        request.setHttpMethod(HttpMethodName.${httpVerb});

        <@MarshalHeaderMembersMacro.content shape shape.variable.variableName/>
        <@UriMemberMarshallerMacro.content shape shape.variable.variableName/>
        <@QueryStringMemberMarshallerMacro.content shape shape.variable.variableName/>

        <#if shape.hasPayloadMember>
            <#list shape.members as member>
                <#if (member.http.isStreaming)>
                request.setContent(${shape.variable.variableName}.${member.getterMethodName}());
                if (!request.getHeaders().containsKey("Content-Type")) {
                    request.addHeader("Content-Type", protocolFactory.getContentType());
                }
                <#elseif (member.http.isPayload) && member.variable.variableType = "software.amazon.awssdk.core.SdkBytes">
                request.setContent(${shape.variable.variableName}.${member.getterMethodName}().asInputStream());
                if (!request.getHeaders().containsKey("Content-Type")) {
                    request.addHeader("Content-Type", protocolFactory.getContentType());
                }
                <#elseif (member.http.isPayload)>
                try {
                    final StructuredJsonGenerator jsonGenerator = protocolFactory.createGenerator();

                    ${member.variable.variableType} ${member.variable.variableName} = ${shape.variable.variableName}.${member.getterMethodName}();
                    if (${member.variable.variableName} != null) {
                    <#if member.isList()>
                        <#local loopVariable = member.variable.variableName + "Value"/>
                        jsonGenerator.writeStartArray();
                        for (${member.listModel.memberType} ${loopVariable} : ${member.variable.variableName}) {
                            if (${loopVariable} != null) {
                            <@ListMemberMacro.content member loopVariable/>
                            }
                        }
                        jsonGenerator.writeEndArray();
                   <#elseif member.isSimple()>
                        jsonGenerator.writeValue(${member.variable.variableName});
                   <#elseif member.isMap()>
                      <#local loopVariable = member.variable.variableName + "Entry"/>
                      jsonGenerator.writeStartObject();
                      for(Map.Entry<${member.mapModel.keyModel.variable.variableType},${member.mapModel.valueModel.variable.variableType}> ${loopVariable} : ${member.variable.variableName}.entrySet()) {
                          if (${loopVariable}.getValue() != null) {
                              jsonGenerator.writeFieldName(${loopVariable}.getKey());

                              <@MapMemberMacro.content member loopVariable+".getValue()"/>
                          }
                      }
                      jsonGenerator.writeEndObject();
                   <#else>
                        jsonGenerator.writeStartObject();
                        <@MemberMarshallerMacro.content customConfig member.c2jShape member.variable.variableName shapes/>
                        jsonGenerator.writeEndObject();
                   </#if>
                    } <@ElseWriteExplicitJsonNull.content/>

                    byte[] content = jsonGenerator.getBytes();
                    request.setContent(new ByteArrayInputStream(content));
                    request.addHeader("Content-Length", Integer.toString(content.length));
                    if (!request.getHeaders().containsKey("Content-Type")) {
                        request.addHeader("Content-Type", protocolFactory.getContentType());
                    }
                } catch(Throwable t) {
                    throw SdkClientException.builder().message("Unable to marshall request to JSON: " + t.getMessage().throwable(t).build();
                }
                <#break>
                </#if>
            </#list>
        <#elseif !shape.unboundMembers?has_content>
        <#-- rest-json requires a zero-byte content if there is no request member bound to the body -->
        request.setContent(new ByteArrayInputStream(new byte[0]));
        if (!request.getHeaders().containsKey("Content-Type")) {
            request.addHeader("Content-Type", protocolFactory.getContentType());
        }
        <#else>
        try {
            final StructuredJsonGenerator jsonGenerator = protocolFactory.createGenerator();
            jsonGenerator.writeStartObject();

            <@MemberMarshallerMacro.content customConfig shapeName shape.variable.variableName shapes/>

            jsonGenerator.writeEndObject();

            byte[] content = jsonGenerator.getBytes();
            request.setContent(new ByteArrayInputStream(content));
            request.addHeader("Content-Length", Integer.toString(content.length));
            if (!request.getHeaders().containsKey("Content-Type")) {
                request.addHeader("Content-Type", protocolFactory.getContentType());
            }
        } catch(Throwable t) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + t.getMessage()).throwable(t).build();
        }
        </#if>

        return request;
    }

    <@RequiredParameterValidationFunctionMacro.content dataModel.customConfig shape/>
}

</#macro>
