${fileHeader}
package ${transformPackage};

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.xml.stream.events.XMLEvent;
import javax.annotation.Generated;

import ${metadata.fullModelPackageName}.*;
import software.amazon.awssdk.awscore.protocol.xml.StaxUnmarshallerContext;
import software.amazon.awssdk.awscore.protocol.xml.SimpleTypeStaxUnmarshallers.*;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.core.runtime.transform.MapEntry;

/**
 * ${shape.shapeName} StAX Unmarshaller
 */

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shape.shapeName}Unmarshaller implements Unmarshaller<${shape.shapeName}, StaxUnmarshallerContext> {

<#assign hasPayload = false>

<#if shape.members?has_content>
<#list shape.members as memberModel>
    <#-- If any member unmarshalls as payload we want to XML unmarshall. Otherwise, response won't contain xml -->
    <#if memberModel.http?has_content>
        <#if memberModel.http.marshallLocation == "PAYLOAD">
            <#assign hasPayload = true>
        </#if>
    </#if>
    <#if memberModel.map>
        <@MapEntryUnmarshallerMacro.content memberModel />
    </#if>
</#list>
</#if>

    public ${shape.shapeName} unmarshall(StaxUnmarshallerContext context) throws Exception {
        ${shape.shapeName}.Builder ${shape.variable.variableName} = ${shape.shapeName}.builder();
        int originalDepth = context.getCurrentDepth();
        int targetDepth = originalDepth + 1;

<#if shape.customization.hasStaxTargetDepthOffset() >
    <#if shape.customization.staxTargetDepthOffset == 0 >
        <#-- if it's zero we can skip applying the offset -->
    <#else>
        if (context.isStartOfDocument()) targetDepth += ${shape.customization.staxTargetDepthOffset};
    </#if>
<#elseif shape.hasPayloadMember>
    <#-- Do not adjust stax context if we need to read raw payload data -->
<#elseif shape.wrapper>
    <#-- For query (elasticache, rds, redshift) protocol, if the return type of certain operation is wrapped, -->
    <#-- it'll have a result wrapper. In the below operaion, the return type is CacheCluster other than CreateCacheClusterResult. -->
    <#-- http://docs.aws.amazon.com/AmazonElastiCache/latest/APIReference/API_CreateCacheCluster.html -->
        if (context.isStartOfDocument()) targetDepth += 3;
<#elseif !shape.unmarshaller.resultWrapper?has_content>
    <#-- For rest-xml (s3, route53, cloudfront) and ec2 protocol, the response data are wrapped by one layer of ???Response tag -->
        if (context.isStartOfDocument()) targetDepth += 1;
<#else>
    <#-- With resultWrapper, the response data is wrapped by two layers: -->
    <#-- http://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_CreateQueue.html -->
        if (context.isStartOfDocument()) targetDepth += 2;
</#if>

<#if shape.hasHeaderMember >
        if (context.isStartOfDocument()) {
    <#list shape.members as memberModel>
        <#if memberModel.http.isHeader() >
            context.setCurrentHeader("${memberModel.http.unmarshallLocationName}");
            ${shape.variable.variableName}.${memberModel.fluentSetterMethodName}(
            <#if memberModel.variable.simpleType == "Instant">
                software.amazon.awssdk.utils.DateUtils.parseRfc1123Date(context.readText()));
            <#else>
                ${memberModel.variable.simpleType}Unmarshaller.getInstance().unmarshall(context));
            </#if>

        </#if>
    </#list>
        }
</#if>

<#if shape.hasStatusCodeMember >
    <#list shape.members as memberModel>
        <#if memberModel.http.isStatusCode() >
        ${shape.variable.variableName}.${memberModel.fluentSetterMethodName}(context.getHttpResponse().getStatusCode());
        </#if>

        <#if !memberModel.http.location?? || memberModel.http.location != "headers">
            <#if memberModel.map>
                java.util.Map<${memberModel.mapModel.keyModel.variable.variableType}, ${memberModel.mapModel.valueModel.variable.variableType}> ${memberModel.variable.variableName} = null;
            </#if>
            <#if memberModel.list>
                java.util.List<${memberModel.listModel.memberType}> ${memberModel.variable.variableName} = null;
            </#if>
        </#if>
    </#list>
</#if>

<#list shape.members as memberModel>
    <#if memberModel.map && memberModel.http.location?? && memberModel.http.location == "headers">
    Map<${memberModel.mapModel.keyModel.variable.variableType}, ${memberModel.mapModel.valueModel.variable.variableType}> ${memberModel.variable.variableName} = new HashMap<>();
    context.getHeaders().entrySet().stream().filter(e -> e.getKey().startsWith("${memberModel.http.unmarshallLocationName}")).forEach(e -> {
        ${memberModel.variable.variableName}.put(e.getKey().replace("${memberModel.http.unmarshallLocationName}", ""), e.getValue());
    });
    ${shape.variable.variableName}.${memberModel.fluentSetterMethodName}(${memberModel.variable.variableName});
    </#if>
</#list>

<#-- If any member unmarshalls as payload we want to XML unmarshall. Otherwise, response won't contain xml -->
<#if hasPayload && !shape.hasStreamingMember >
<#list shape.members as memberModel>
    <#if !memberModel.http.location?? || memberModel.http.location != "headers">
        <#if memberModel.map>
            java.util.Map<${memberModel.mapModel.keyModel.variable.variableType}, ${memberModel.mapModel.valueModel.variable.variableType}> ${memberModel.variable.variableName} = null;
        </#if>
        <#if memberModel.list>
            java.util.List<${memberModel.listModel.memberType}> ${memberModel.variable.variableName} = null;
        </#if>
    </#if>
</#list>

        while (true) {
            XMLEvent xmlEvent = context.nextEvent();
            if (xmlEvent.isEndDocument()) {
            <#-- Set any map members we filled during unmarshalling -->
<#list shape.members as memberModel>
    <#if memberModel.map || memberModel.list>
        ${shape.variable.variableName}.${memberModel.fluentSetterMethodName}(${memberModel.variable.variableName});
    </#if>
</#list>
                break;
            }

            if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {

<#if shape.members?has_content>
  <#if shape.customization.artificialResultWrapper?has_content>
    <#assign artificialWrapper = shape.customization.artificialResultWrapper />
    <#assign setterMethod = shape.getMemberByName(artificialWrapper.wrappedMemberName).fluentSetterMethodName />
    <#-- If it's a result wrapper created by the customization, then we skip xpath test and directly invoke the unmarshaller for the wrapped member -->
                ${shape.variable.variableName}.${setterMethod}(
                    ${artificialWrapper.wrappedMemberSimpleType}Unmarshaller.getInstance().unmarshall(context)
                    );
                continue;
  <#else>
    <#list shape.members as memberModel>
        <#if !memberModel.http.isHeader() && !memberModel.http.isStatusCode() >
            <@MemberUnmarshallerInvocationMacro.content shape.variable.variableName memberModel />
        </#if>
    </#list>
  </#if>
</#if>
            } else if (xmlEvent.isEndElement()) {
                if (context.getCurrentDepth() < originalDepth) {
<#list shape.members as memberModel>
    <#if memberModel.map || memberModel.list>
        ${shape.variable.variableName}.${memberModel.fluentSetterMethodName}(${memberModel.variable.variableName});
    </#if>
</#list>
                    break;
                }
            }
        }
</#if>
        <#-- If any member unmarshalls as payload we want to XML unmarshall. Otherwise, response won't contain xml -->
        return ${shape.variable.variableName}.build();
    }

    private static final ${shape.shapeName}Unmarshaller INSTANCE = new ${shape.shapeName}Unmarshaller();
    public static ${shape.shapeName}Unmarshaller getInstance() {
        return INSTANCE;
    }
}
