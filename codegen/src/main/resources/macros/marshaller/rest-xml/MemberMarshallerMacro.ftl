<#macro content customConfig shapeName getterFunctionPrefix shapes>
<#local shape = shapes[shapeName]/>

<#if shape.members?has_content>
<#list shape.members as member>
<#local getMember = getterFunctionPrefix + "." + member.fluentGetterMethodName />
<#local http = member.http />

<#if !(http.location)?? && !(http.isStreaming) && !(http.isPayload)>
<#if member.simple>
    <#if member.idempotencyToken>
        xmlWriter.startElement("${http.marshallLocationName}").value(<@IdempotencyTokenMacro.content getMember member.variable.simpleType/>).endElement();
    <#else>
        if(${getMember}() != null) {
            xmlWriter.startElement("${http.marshallLocationName}").value(${getMember}()).endElement();
        }
    </#if>
<#elseif member.list>
    <#local listModel = member.listModel />
    <#-- More explicit name in order to avoid variable name collision -->
    <#local listVariable = shapeName?uncap_first + member.name + "List"/>
    <#local loopVariable = listVariable + "Value"/>

      ${listModel.templateType} ${listVariable} = ${getMember}();
  <#if customConfig.useAutoConstructList>
      if (!${listVariable}.isEmpty() || !(${listVariable} instanceof software.amazon.awssdk.core.util.SdkAutoConstructList)) {
  <#else>
      if (${listVariable} != null) {
  </#if>
  <#if member.http.flattened>
      for (${listModel.memberType} ${loopVariable} : ${listVariable}) {
          <#local memberLocationName = listModel.memberLocationName!http.marshallLocationName />
          xmlWriter.startElement("${memberLocationName}");
          <#if listModel.simple>
              xmlWriter.value(${loopVariable});
          <#else>
              <@MemberMarshallerMacro.content customConfig listModel.memberType loopVariable shapes/>
          </#if>
          xmlWriter.endElement();
      }
     }
  <#else>
      xmlWriter.startElement("${http.marshallLocationName}");

      for (${listModel.memberType} ${loopVariable} : ${listVariable}) {
          <#local memberLocationName = listModel.memberLocationName!"member" />
          xmlWriter.startElement("${memberLocationName}");
          <#if listModel.simple>
              xmlWriter.value(${loopVariable});
          <#else>
              <@MemberMarshallerMacro.content customConfig listModel.memberType loopVariable shapes/>
          </#if>
          xmlWriter.endElement();
      }
      xmlWriter.endElement();
    }
  </#if>
<#elseif member.map>
    <#local mapModel = member.mapModel />
    <#-- More explicit name in order to avoid variable name collision -->
    <#local mapVariable = shapeName?uncap_first + member.name + "Map"/>
    <#local loopVariable = mapVariable + "Value"/>

    ${mapModel.templateType} ${mapVariable} = (${mapModel.templateType})${getMember}();
    if (${mapVariable} != null) {
        xmlWriter.startElement("${http.marshallLocationName}");

        for (${mapModel.entryType} ${loopVariable} : ${mapVariable}.entrySet()) {
            xmlWriter.startElement("entry");
            xmlWriter.startElement("${mapModel.keyLocationName}");
            xmlWriter.value(${loopVariable}.getKey());
            xmlWriter.endElement();
            xmlWriter.startElement("${mapModel.valueLocationName}");
            <#if mapModel.valueModel.simple>
                xmlWriter.value(${loopVariable}.getValue());
            <#else>
                <@MemberMarshallerMacro.content customConfig mapModel.valueModel.variable.variableType loopVariable shapes/>
            </#if>
            xmlWriter.endElement();
            xmlWriter.endElement();
        }
        xmlWriter.endElement();
    }
<#else>
    <#local variable = member.variable />
    ${variable.variableType} ${variable.variableName} = ${getMember}();
    if (${variable.variableName} != null) {
        xmlWriter.startElement("${http.marshallLocationName}");
        <@MemberMarshallerMacro.content customConfig variable.simpleType variable.variableName shapes/>
        xmlWriter.endElement();
    }
</#if>
</#if>
</#list>
</#if>
</#macro>
