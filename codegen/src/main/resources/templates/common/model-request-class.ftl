${fileHeader}
package ${metadata.fullModelPackageName};

import javax.annotation.Generated;

import software.amazon.awssdk.AmazonWebServiceRequest;
<#if shape.requestSignerAware>
import software.amazon.awssdk.auth.RequestSigner;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerAware;
</#if>

<#if shape.documentation?has_content || awsDocsUrl?has_content>
/**
<#if shape.documentation?has_content> * ${shape.documentation}</#if>
<#if awsDocsUrl?has_content> * ${awsDocsUrl}</#if>
 */
</#if>
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shape.shapeName} extends ${baseClassFqcn} implements Cloneable
        <#if shape.requestSignerAware>, RequestSignerAware</#if> {

    <@VariableDeclarationMacro.content shape/>

    <#if shape.additionalConstructors?has_content>
        <@ConstructorDefinitionMacro.content shape/>
    </#if>

    <@MethodDefinitionMacro.content customConfig shape shape.shapeName/>

    <#-- Injection point for adding custom code to request classes -->
    <#if CustomRequestClassMethodsMacro?has_content>
        <@CustomRequestClassMethodsMacro.content shape/>
    </#if>

    <@OverrideMethodsMacro.content shape />

    @Override
    public ${shape.shapeName} clone() {
        return (${shape.shapeName}) super.clone();
    }

    <#if shape.requestSignerAware>
    @Override
    public Class<? extends RequestSigner> signerType() {
        return ${shape.requestSignerClassFqcn}.class;
    }
    </#if>

    <#if shouldGenerateSdkRequestConfigSetter>
    /**
     * Set the configuration for this request.
     *
     * @param sdkRequestConfig Request configuration.
     * @return This object for method chaining.
     */
    public ${shape.shapeName} sdkRequestConfig(software.amazon.awssdk.opensdk.SdkRequestConfig sdkRequestConfig) {
        super.sdkRequestConfig(sdkRequestConfig);
        return this;
    }
    </#if>

}
