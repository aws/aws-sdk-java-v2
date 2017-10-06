<#macro content member>
    <#if member.isIdempotencyToken()>
    .defaultValueSupplier(software.amazon.awssdk.core.util.IdempotentUtils.getGenerator())
    </#if>
</#macro>
