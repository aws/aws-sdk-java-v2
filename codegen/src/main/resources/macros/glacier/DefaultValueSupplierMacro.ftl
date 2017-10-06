<#macro content member>
    <#-- If account id is not specified, we default to '-' which indicates the current account -->
    <#if (member.http.uri && member.name == "AccountId")>
    .defaultValueSupplier(DefaultAccountIdSupplier.getInstance())
    <#elseif member.isIdempotencyToken()>
    .defaultValueSupplier(software.amazon.awssdk.core.util.IdempotentUtils.getGenerator())
    </#if>
</#macro>
