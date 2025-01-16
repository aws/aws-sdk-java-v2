/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.poet.auth.scheme;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.utils.CollectionUtils;

public final class AuthSchemeSpecUtils {
    private static final Set<String> DEFAULT_AUTH_SCHEME_PARAMS = setOf("region", "operation");
    private final IntermediateModel intermediateModel;
    private final boolean useSraAuth;
    private final Set<String> allowedEndpointAuthSchemeParams;
    private final boolean allowedEndpointAuthSchemeParamsConfigured;

    public AuthSchemeSpecUtils(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        CustomizationConfig customization = intermediateModel.getCustomizationConfig();
        this.useSraAuth = customization.useSraAuth();
        if (customization.getAllowedEndpointAuthSchemeParamsConfigured()) {
            this.allowedEndpointAuthSchemeParams = Collections.unmodifiableSet(
                new HashSet<>(customization.getAllowedEndpointAuthSchemeParams()));
            this.allowedEndpointAuthSchemeParamsConfigured = true;
        } else {
            this.allowedEndpointAuthSchemeParams = Collections.emptySet();
            this.allowedEndpointAuthSchemeParamsConfigured = false;
        }
    }

    public boolean useSraAuth() {
        return useSraAuth;
    }

    private String basePackage() {
        return intermediateModel.getMetadata().getFullAuthSchemePackageName();
    }

    private String internalPackage() {
        return intermediateModel.getMetadata().getFullInternalAuthSchemePackageName();
    }

    public String baseClientPackageName() {
        return intermediateModel.getMetadata().getFullClientPackageName();
    }

    public ClassName parametersInterfaceName() {
        return ClassName.get(basePackage(), intermediateModel.getMetadata().getServiceName() + "AuthSchemeParams");
    }

    public ClassName parametersEndpointAwareDefaultImplName() {
        return ClassName.get(internalPackage(), intermediateModel.getMetadata().getServiceName() + "EndpointResolverAware");
    }

    public ClassName parametersInterfaceBuilderInterfaceName() {
        return parametersInterfaceName().nestedClass("Builder");
    }

    public ClassName parametersDefaultImplName() {
        return ClassName.get(internalPackage(), "Default" + parametersInterfaceName().simpleName());
    }

    public ClassName parametersDefaultBuilderImplName() {
        return ClassName.get(internalPackage(), "Default" + parametersInterfaceName().simpleName());
    }

    public ClassName providerInterfaceName() {
        return ClassName.get(basePackage(), intermediateModel.getMetadata().getServiceName() + "AuthSchemeProvider");
    }

    public ClassName defaultAuthSchemeProviderName() {
        return ClassName.get(internalPackage(), "Default" + providerInterfaceName().simpleName());
    }

    public ClassName modeledAuthSchemeProviderName() {
        return ClassName.get(internalPackage(), "Modeled" + providerInterfaceName().simpleName());
    }

    public ClassName authSchemeInterceptor() {
        return ClassName.get(internalPackage(), intermediateModel.getMetadata().getServiceName() + "AuthSchemeInterceptor");
    }

    public TypeName resolverReturnType() {
        return ParameterizedTypeName.get(List.class, AuthSchemeOption.class);
    }

    public boolean usesSigV4() {
        return AuthUtils.usesAwsAuth(intermediateModel);
    }

    public boolean usesSigV4a() {
        return AuthUtils.usesSigv4aAuth(intermediateModel);
    }

    public boolean useEndpointBasedAuthProvider() {
        // Endpoint based auth provider is gated using the same setting that enables the use of auth scheme params. One does
        // not make sense without the other so there's no much point on creating another setting if both have to be at the same
        // time enabled or disabled.
        return generateEndpointBasedParams();
    }

    public String paramMethodName(String name) {
        return intermediateModel.getNamingStrategy().getVariableName(name);
    }

    public boolean generateEndpointBasedParams() {
        return intermediateModel.getCustomizationConfig().isEnableEndpointAuthSchemeParams();
    }

    public boolean includeParam(String name) {
        if (allowedEndpointAuthSchemeParamsConfigured) {
            return allowedEndpointAuthSchemeParams.contains(name);
        }
        // If no explicit allowed endpoint auth scheme params are configured then by default we include all of them except the
        // ones already defined by default.
        return !DEFAULT_AUTH_SCHEME_PARAMS.contains(name.toLowerCase(Locale.US));
    }

    public boolean includeParamForProvider(String name) {
        if (allowedEndpointAuthSchemeParamsConfigured) {
            if (DEFAULT_AUTH_SCHEME_PARAMS.contains(name.toLowerCase(Locale.US))) {
                return true;
            }
            return allowedEndpointAuthSchemeParams.contains(name);
        }
        return true;
    }

    //Multi-Auth option determined by "auth" trait on Service model or operation model.
    public boolean hasMultiAuthSigvOrSigv4a() {
        List<AuthType> authList = intermediateModel.getMetadata().getAuth();

        return (!CollectionUtils.isNullOrEmpty(authList) &&
                authList.stream().anyMatch(authType -> authType == AuthType.V4 || authType == AuthType.V4A))
               ||
               intermediateModel.getOperations()
                                .values()
                                .stream()
                                .flatMap(operationModel -> operationModel.getAuth().stream())
                                .anyMatch(authType -> authType == AuthType.V4 || authType == AuthType.V4A);
    }

    //Include Endpoint params in Auth Schemes to resolve the Endpoint for obtaining Signing properties in Multi Auth.
    public boolean useEndpointParamsInAuthScheme() {
        return generateEndpointBasedParams() || hasMultiAuthSigvOrSigv4a();
    }

    public String serviceName() {
        return intermediateModel.getMetadata().getServiceName();
    }

    public String signingName() {
        return intermediateModel.getMetadata().getSigningName();
    }

    private static Set<String> setOf(String val1, String val2) {
        Set<String> result = new HashSet<>();
        result.add(val1);
        result.add(val2);
        return Collections.unmodifiableSet(result);
    }
}
