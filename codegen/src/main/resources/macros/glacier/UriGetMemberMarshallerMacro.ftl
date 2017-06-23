<#macro content getterFunctionPrefix, member>
    <#local getterCall=getterFunctionPrefix + "." + member.getterMethodName + "()"/>
    <#-- If account id is not specified, we default to '-' which indicates the current account -->
    <#if (member.http.uri && member.name == "AccountId")>
        ${getterCall} == null ? "-" : ${getterCall}
    <#else>
        ${getterCall}
    </#if>
</#macro>
