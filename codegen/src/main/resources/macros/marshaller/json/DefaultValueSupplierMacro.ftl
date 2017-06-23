<#macro content member>
    <#if member.isIdempotencyToken()>
    .defaultValueSupplier(software.amazon.awssdk.util.IdempotentUtils.getGenerator())
    </#if>
</#macro>
