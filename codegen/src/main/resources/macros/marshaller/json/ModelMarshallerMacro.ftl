<#macro content dataModel>
<#local shapes = dataModel.shapes/>
<#local metadata = dataModel.metadata/>
<#local shapeName = dataModel.shapeName/>
<#local customConfig = dataModel.customConfig/>

${dataModel.fileHeader}
package ${dataModel.transformPackage};

import java.util.Map;
import java.util.List;
import javax.annotation.Generated;

import software.amazon.awssdk.core.exception.SdkClientException;
import ${metadata.fullModelPackageName}.*;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.core.protocol.json.*;

/**
 * ${shapeName}Marshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shapeName}JsonMarshaller {

    <#assign shape = shapes[shapeName]/>
    /**
     * Marshall the given parameter object, and output to a SdkJsonGenerator
     */
    public void marshall(${shapeName} ${shape.variable.variableName}, StructuredJsonGenerator jsonGenerator) {

        if (${shape.variable.variableName} == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        <@RequiredParameterValidationInvocationMacro.content dataModel.customConfig shape/>

        try {
            jsonGenerator.writeStartObject();

            <@MemberMarshallerMacro.content customConfig shapeName shape.variable.variableName shapes/>

            jsonGenerator.writeEndObject();
        } catch(Throwable t) {
            throw new SdkClientException("Unable to marshall request to JSON: " + t.getMessage(), t);
        }
    }

    private static final ${shapeName}JsonMarshaller INSTANCE = new ${shapeName}JsonMarshaller();
    public static ${shapeName}JsonMarshaller getInstance() {
        return INSTANCE;
    }

    <@RequiredParameterValidationFunctionMacro.content dataModel.customConfig shape/>
}
</#macro>
