<#assign shapes = shapes/>
<#assign metadata = metadata/>
<#assign shapeName = shapeName/>
<#assign customConfig = customConfig/>
<#assign shape = shapes[shapeName]/>

${fileHeader}
package ${transformPackage};

import java.util.Map;
import java.util.List;
import javax.annotation.Generated;

import software.amazon.awssdk.core.SdkClientException;
import ${metadata.fullModelPackageName}.*;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.core.protocol.*;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * ${shapeName}Marshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
@SdkInternalApi
public class ${className} {

    <#if shape.nonStreamingMembers??>
        <#list shape.nonStreamingMembers as member>
            private static final MarshallingInfo<${member.marshallingTargetClass}> ${member.marshallerBindingFieldName} = MarshallingInfo.builder(MarshallingType.${member.marshallingType})
                 .marshallLocation(MarshallLocation.${member.http.marshallLocation})
                 <#if member.http.isPayload>
                 .isExplicitPayloadMember(true)
                 <#else>
                 .marshallLocationName("${member.http.marshallLocationName}")
                 </#if>
                 <#if member.isBinary>
                 .isBinary(true)
                 </#if>
                 <@DefaultValueSupplierMacro.content member />
                 .build();
        </#list>
    </#if>

    private static final ${className} INSTANCE = new ${className}();

    public static ${className} getInstance() {
        return INSTANCE;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(${shapeName} ${shape.variable.variableName}, ProtocolMarshaller protocolMarshaller) {

        if (${shape.variable.variableName} == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            <#if shape.nonStreamingMembers??>
                <#list shape.nonStreamingMembers as member>
                <#assign getter = shape.variable.variableName + "." + member.fluentGetterMethodName + "()" />

                protocolMarshaller.marshall(
                ${getter},
                ${member.marshallerBindingFieldName});
                </#list>
            </#if>
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

    <@RequiredParameterValidationFunctionMacro.content customConfig shape/>
}

