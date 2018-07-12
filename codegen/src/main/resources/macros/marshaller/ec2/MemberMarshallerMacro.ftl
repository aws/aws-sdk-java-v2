<#macro content customConfig shapeName getterFunctionPrefix, shapes, contextPath>
<#local shape = shapes[shapeName]/>

<#if shape.members??>
    <#list shape.members as member>
        <#local getMember = getterFunctionPrefix + "." + member.fluentGetterMethodName />
        <#local marshallLocationName = member.http.marshallLocationName />

        <#if member.http.additionalMarshallingPath?has_content>
            <#local marshallLocationName = marshallLocationName + "." + member.http.additionalMarshallingPath />
        </#if>

        <#if contextPath?has_content>
            <#local marshallLocationName = contextPath + "." + marshallLocationName/>
        </#if>

        <#if member.simple>
            <#if member.idempotencyToken>
                request.addParameter("${marshallLocationName}", <@IdempotencyTokenMacro.content getMember member.variable.simpleType/>);
            <#else>
                if(${getMember}() != null) {
                    request.addParameter("${marshallLocationName}", StringConversion.from${member.variable.simpleType}(${getMember}()));
                }
            </#if>
        <#elseif member.list>
            <#local listModel = member.listModel />
            <#local listMemberType = listModel.memberType/>
            <#local memberVariableName = member.name?uncap_first/>
            <#local listIndex = memberVariableName  + "ListIndex"/>
            <#local listVariable = shape.variable.variableName + memberVariableName?cap_first + "List"/>
            <#local loopVariable = listVariable + "Value"/>

            ${listModel.templateType} ${listVariable} = ${getMember}();
            <#if customConfig.useAutoConstructList>
                if (!${listVariable}.isEmpty() || !(${listVariable} instanceof software.amazon.awssdk.core.util.SdkAutoConstructList)) {
            <#else>
                if (${listVariable} != null) {
            </#if>
                int ${listIndex} = 1;

                for (${listModel.memberType} ${loopVariable} : ${listVariable}) {
                    <#if listModel.simple>
                         if (${loopVariable} != null) {
                            <#if listModel.memberAdditionalMarshallingPath?has_content>
                                request.addParameter("${marshallLocationName}." + ${listIndex} + ".${listModel.memberAdditionalMarshallingPath}", StringConversion.from${listModel.memberType}(${loopVariable}));
                            <#else>
                                request.addParameter("${marshallLocationName}." + ${listIndex}, StringConversion.from${listModel.memberType}(${loopVariable}));
                            </#if>
                         }
                    <#else>
                        <#if listModel.memberAdditionalMarshallingPath?has_content>
                            <@MemberMarshallerMacro.content customConfig listModel.memberType loopVariable shapes marshallLocationName + ".\" + " + listIndex + " + \"."+listModel.memberAdditionalMarshallingPath+"\""/>
                        <#else>
                            <@MemberMarshallerMacro.content customConfig listModel.memberType loopVariable shapes marshallLocationName + ".\" + " + listIndex + " + \""/>
                        </#if>
                    </#if>
                    ${listIndex}++;
                }
            }
        <#else>
            <#local memberVariableName = member.variable.variableName/>
            ${member.variable.variableType}  ${memberVariableName} = ${getMember}();
            if (${memberVariableName} != null) {
                <@MemberMarshallerMacro.content customConfig member.variable.variableType memberVariableName shapes marshallLocationName/>
            }
        </#if>
    </#list>
</#if>
</#macro>
