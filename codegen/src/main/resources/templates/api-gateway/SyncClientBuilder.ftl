${fileHeader}
package ${metadata.fullClientPackageName};

import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.opensdk.protect.client.SdkSyncClientBuilder;
import software.amazon.awssdk.opensdk.internal.config.ApiGatewayLegacyClientConfigurationFactory;
<#if metadata.requiresIamSigners>
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.Signer;
</#if>
<#list customAuthorizers?values as customSigner>
import ${metadata.fullAuthPolicyPackageName}.${customSigner.interfaceName};
</#list>
import software.amazon.awssdk.util.RuntimeHttpUtils;
import software.amazon.awssdk.Protocol;

import java.net.URI;
import javax.annotation.Generated;

/**
 * Fluent builder for {@link ${metadata.fullClientPackageName + "." + metadata.syncInterface}}. Use of the
 * builder is preferred over using constructors of the client class.
**/
@NotThreadSafe
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public final class ${metadata.syncClientBuilderClassName}
    extends SdkSyncClientBuilder<${metadata.syncClientBuilderClassName}, ${metadata.syncInterface}> {

    private static final URI DEFAULT_ENDPOINT = RuntimeHttpUtils.toUri("${metadata.defaultEndpoint}", Protocol.HTTPS);
    private static final String DEFAULT_REGION = "${metadata.defaultRegion}";

    /**
     * Package private constructor - builder should be created via {@link ${metadata.syncInterface}#builder()}
     */
    ${metadata.syncClientBuilderClassName}() {
        super(new ApiGatewayLegacyClientConfigurationFactory());
    }

    <#if metadata.requiresApiKey>
    /**
     * Specify the API Key to send with requests.
     *
     * @param apiKey API Key that identifies caller.
     * @return This object for method chaining.
     */
    @Override
    public void setApiKey(String apiKey) {
        super.setApiKey(apiKey);
    }

    /**
     * Specify the API Key to send with requests.
     *
     * @param apiKey API Key that identifies caller.
     * @return This object for method chaining.
     */
    public ${metadata.syncClientBuilderClassName} apiKey(String apiKey) {
        setApiKey(apiKey);
        return this;
    }
    </#if>

    <#if metadata.requiresIamSigners>
    /**
     * Specify an implementation of {@link AwsCredentialsProvider} to be used
     * when signing IAM auth'd requests
     *
     * @param iamCredentials the credential provider
     */
    @Override
    public void setIamCredentials(AwsCredentialsProvider iamCredentials) {
        super.setIamCredentials(iamCredentials);
    }

    /**
     * Specify an implementation of {@link AwsCredentialsProvider} to be used
     * when signing IAM auth'd requests
     *
     * @param iamCredentials the credential provider
     * @return This object for method chaining.
     */
    public ${metadata.syncClientBuilderClassName} iamCredentials(AwsCredentialsProvider iamCredentials) {
        setIamCredentials(iamCredentials);
        return this;
    }

    /**
     * Sets the IAM region to use when using IAM auth'd requests
     * against a service in any of it's non-default regions. This is only
     * expected to be used when a custom endpoint has also been set.
     *
     * @param iamRegion the IAM region string to use when signing
     */
    public void setIamRegion(String iamRegion) {
        super.setIamRegion(iamRegion);
    }

    /**
     * Sets the IAM region to use when using IAM auth'd requests
     * against a service in any of it's non-default regions. This is only
     * expected to be used when a custom endpoint has also been set.
     *
     * @param iamRegion the IAM region string to use when signing
     * @return This object for method chaining.
     */
    public ${metadata.syncClientBuilderClassName} iamRegion(String iamRegion) {
        setIamRegion(iamRegion);
        return this;
    }
    </#if>

    <#list customAuthorizers?values as customSigner>
    /**
     * Specify an implementation of the ${customSigner.interfaceName} to be used during signing
     * @param requestSigner the requestSigner implementation to use
     * @return This object for method chaining.
     */
    public ${metadata.syncClientBuilderClassName} signer(${customSigner.interfaceName} requestSigner) {
        return signer(requestSigner , ${customSigner.interfaceName}.class);
    }

    /**
     * Specify an implementation of the ${customSigner.interfaceName} to be used during signing
     * @param requestSigner the requestSigner implementation to use
     */
    public void setSigner(${customSigner.interfaceName} requestSigner) {
        signer(requestSigner);
    }
    </#list>

    /**
     * Construct a synchronous implementation of ${metadata.syncInterface} using the current builder configuration.
     *
     * @param params Current builder configuration represented as a parameter object.
     * @return Fully configured implementation of ${metadata.syncInterface}.
     */
    @Override
    protected ${metadata.syncInterface} build(AwsSyncClientParams params) {
        return new ${metadata.syncClient}(params);
    }

    /**
    * @return Create new instance of builder with all defaults set.
    */
    public static ${metadata.syncClientBuilderClassName} standard() {
        return new ${metadata.syncClientBuilderClassName}();
    }

    @Override
    protected URI defaultEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    @Override
    protected String defaultRegion() {
        return DEFAULT_REGION;
    }

    <#if metadata.requiresIamSigners>
    @Override
    protected Signer defaultIamSigner() {
        return signerFactory().createSigner();
    }
    </#if>
}
