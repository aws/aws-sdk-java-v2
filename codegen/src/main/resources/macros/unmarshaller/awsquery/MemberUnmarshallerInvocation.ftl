<#macro content shapeVarName memberModel >

<#local unmarshallerLocationName = memberModel.http.unmarshallLocationName />
<#if memberModel.http.additionalUnmarshallingPath?has_content>
    <#local unmarshallerLocationName = unmarshallerLocationName + "/" + memberModel.http.additionalUnmarshallingPath />
</#if>

<#if memberModel.list>
    <#if memberModel.http.flattened>
        <#local listMemberPath = memberModel.listModel.memberLocationName!memberModel.http.unmarshallLocationName!memberModel.name />
    <#else>
        <#local listMemberPath = unmarshallerLocationName + "/" + memberModel.listModel.memberLocationName!"member" />
    </#if>

    <#if memberModel.listModel.memberAdditionalUnmarshallingPath?has_content>
        <#local listMemberPath = listMemberPath + "/" + memberModel.listModel.memberAdditionalUnmarshallingPath />
    </#if>

            <#if !memberModel.http.flattened>
                if (context.testExpression("${unmarshallerLocationName}", targetDepth)) {
                    ${shapeVarName}.${memberModel.fluentSetterMethodName}(new ArrayList<${memberModel.listModel.memberType}>());
                    continue;
                }
            </#if>

                if (context.testExpression("${listMemberPath}", targetDepth)) {
                    ${shapeVarName}.${memberModel.fluentSetterMethodName}(${memberModel.listModel.simpleType}Unmarshaller.getInstance().unmarshall(context));
                    continue;
                }

<#elseif memberModel.map && (!memberModel.http.location?? || memberModel.http.location != "headers")>
    <#local mapEntryPath = unmarshallerLocationName />

    <#if !memberModel.http.flattened>
        <#local mapEntryPath = "${unmarshallerLocationName}/entry" />
    </#if>
                if (context.testExpression("${mapEntryPath}", targetDepth)) {
                    if (${memberModel.variable.variableName} == null) {
                        ${memberModel.variable.variableName} = new java.util.HashMap<>();
                    }
                    Entry<${memberModel.mapModel.keyType}, ${memberModel.mapModel.valueType}> entry = ${memberModel.name}MapEntryUnmarshaller.getInstance().unmarshall(context);
                    // ${shapeVarName}.add${memberModel.name}Entry(entry.getKey(), entry.getValue());

                    ${memberModel.variable.variableName}.put(entry.getKey(), entry.getValue());
                    continue;
                }

<#elseif !memberModel.map>
                if (context.testExpression("${unmarshallerLocationName}", targetDepth)) {
                    ${shapeVarName}.${memberModel.fluentSetterMethodName}(${memberModel.variable.simpleType}Unmarshaller.getInstance().unmarshall(context));
                    continue;
                }
</#if>
</#macro>
