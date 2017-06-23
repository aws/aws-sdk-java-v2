<#macro content getterFunctionPrefix, member>
    ${getterFunctionPrefix + "." + member.fluentGetterMethodName + "()"}
</#macro>
