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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.http.auth.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;

public final class AuthSchemeSpecUtils {
    private static final Set<String> DEFAULT_AUTH_SCHEME_PARAMS = Collections.unmodifiableSet(setOf("region", "operation"));
    private final IntermediateModel intermediateModel;
    private final Set<String> allowedEndpointAuthSchemeParams;
    private final boolean allowedEndpointAuthSchemeParamsConfigured;

    public AuthSchemeSpecUtils(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        CustomizationConfig customization = intermediateModel.getCustomizationConfig();
        if (customization.getAllowedEndpointAuthSchemeParamsConfigured()) {
            this.allowedEndpointAuthSchemeParams = Collections.unmodifiableSet(
                new HashSet<>(customization.getAllowedEndpointAuthSchemeParams()));
            this.allowedEndpointAuthSchemeParamsConfigured = true;
        } else {
            this.allowedEndpointAuthSchemeParams = Collections.emptySet();
            this.allowedEndpointAuthSchemeParamsConfigured = false;
        }
    }

    private String basePackage() {
        return intermediateModel.getMetadata().getFullAuthSchemePackageName();
    }

    private String internalPackage() {
        return intermediateModel.getMetadata().getFullInternalAuthSchemePackageName();
    }

    public ClassName parametersInterfaceName() {
        return ClassName.get(basePackage(), intermediateModel.getMetadata().getServiceName() + "AuthSchemeParams");
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

    public String serviceName() {
        return intermediateModel.getMetadata().getServiceName();
    }

    public String signingName() {
        return intermediateModel.getMetadata().getSigningName();
    }

    public Map<List<String>, List<AuthType>> operationsToAuthType() {
        Map<List<AuthType>, List<String>> authSchemesToOperations =
            intermediateModel.getOperations()
                             .entrySet()
                             .stream()
                             .filter(kvp -> !kvp.getValue().getAuth().isEmpty())
                             .collect(Collectors.groupingBy(kvp -> kvp.getValue().getAuth(),
                                                            Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        Map<List<String>, List<AuthType>> operationsToAuthType = authSchemesToOperations
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(left -> left.getValue().get(0)))
            .collect(Collectors.toMap(Map.Entry::getValue,
                                      Map.Entry::getKey, (a, b) -> b,
                                      LinkedHashMap::new));

        List<AuthType> serviceDefaults = serviceDefaultAuthTypes();

        // Get the list of operations that share the same auth schemes as the system defaults and remove it from the result. We
        // will take care of all of these in the fallback `default` case.
        List<String> operationsWithDefaults = authSchemesToOperations.remove(serviceDefaults);
        operationsToAuthType.remove(operationsWithDefaults);
        operationsToAuthType.put(Collections.emptyList(), serviceDefaults);
        return operationsToAuthType;
    }

    public List<AuthType> serviceDefaultAuthTypes() {
        List<AuthType> modeled = intermediateModel.getMetadata().getAuth();
        if (!modeled.isEmpty()) {
            return modeled;
        }
        return Collections.singletonList(intermediateModel.getMetadata().getAuthType());
    }

    public Set<Class<?>> allServiceConcreteAuthSchemeClasses() {
        Set<Class<?>> result =
            Stream.concat(intermediateModel.getOperations()
                                           .values()
                                           .stream()
                                           .map(OperationModel::getAuth)
                                           .flatMap(List::stream),
                          intermediateModel.getMetadata().getAuth().stream())
                  .map(AuthSchemeCodegenMetadata::fromAuthType)
                  .map(AuthSchemeCodegenMetadata::authSchemeClass)
                  .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Class::getSimpleName))));

        if (useEndpointBasedAuthProvider()) {
            // sigv4a is not modeled but needed for the endpoints based auth-scheme cases.
            result.add(AwsV4aAuthScheme.class);
        }
        // Make the no-auth scheme available.
        result.add(NoAuthAuthScheme.class);
        return result;
    }

    private static Set<String> setOf(String v1, String v2) {
        Set<String> set = new HashSet<>();
        set.add(v1);
        set.add(v2);
        return set;
    }
}
