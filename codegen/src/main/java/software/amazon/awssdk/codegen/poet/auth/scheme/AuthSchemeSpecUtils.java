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
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;

public final class AuthSchemeSpecUtils {
    private static final Set<String> DEFAULT_AUTH_SCHEME_PARAMS = Collections.unmodifiableSet(setOf("region", "operation"));
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

    /**
     * Computes a map from operations to codegen metadata objects. The intermediate model is used to compute mappings to
     * {@link AuthType} values for the service and for each operation that has an override. Then we group all the operations
     * that share the same set of auth types together and finally convert the auth types to their corresponding codegen
     * metadata instances that then we can use to codegen switch statements. The service wide codegen metadata instances are
     * keyed using {@link Collections#emptyList()}.
     *
     * @see #computeServiceWideDefaults
     */
    private Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToModeledMetadata() {
        Map<List<String>, List<AuthType>> operationsToAuthType = operationsToAuthType();
        Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToMetadata = new LinkedHashMap<>();
        operationsToAuthType.forEach((k, v) -> operationsToMetadata.put(k, authTypeToCodegenMetadata(v)));
        return operationsToMetadata;
    }

    public Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToMetadata() {
        List<AuthType> serviceDefaults = serviceDefaultAuthTypes();
        if (serviceDefaults.size() == 1) {
            String authTypeName = serviceDefaults.get(0).value();
            SigV4SignerDefaults defaults = AuthTypeToSigV4Default.authTypeToDefaults().get(authTypeName);
            if (areServiceWide(defaults)) {
                return computeServiceWideDefaults(defaults);
            }
        }
        return operationsToModeledMetadata();
    }

    /**
     * Similar to {@link #operationsToModeledMetadata()} computes a map from operations to codegen metadata objects. The
     * service default list of codegen metadata is keyed with {@link Collections#emptyList()}.
     *
     * This map is used to codegen switch statements.
     */
    private Map<List<String>, List<AuthSchemeCodegenMetadata>> computeServiceWideDefaults(SigV4SignerDefaults defaults) {
        Map<SigV4SignerDefaults, List<String>> defaultsToOperations =
            defaults.operations()
                    .entrySet()
                    .stream()
                    .map(kvp -> new AbstractMap.SimpleEntry<>(kvp.getKey(), kvp.getValue()))
                    .collect(Collectors.groupingBy(AbstractMap.SimpleEntry::getValue,
                                                   Collectors.mapping(AbstractMap.SimpleEntry::getKey,
                                                                      Collectors.toList())));

        Map<List<String>, SigV4SignerDefaults> operationsToDefaults =
            defaultsToOperations.entrySet()
                                .stream()
                                .sorted(Comparator.comparing(left -> left.getValue().get(0)))
                                .collect(Collectors.toMap(Map.Entry::getValue,
                                                          Map.Entry::getKey, (a, b) -> b,
                                                          LinkedHashMap::new));

        Map<List<String>, List<AuthSchemeCodegenMetadata>> result = new LinkedHashMap<>();
        for (Map.Entry<List<String>, SigV4SignerDefaults> kvp : operationsToDefaults.entrySet()) {
            result.put(kvp.getKey(),
                       Arrays.asList(AuthSchemeCodegenMetadata.fromConstants(kvp.getValue())));
        }
        result.put(Collections.emptyList(), Arrays.asList(AuthSchemeCodegenMetadata.fromConstants(defaults)));
        return result;
    }

    public boolean areServiceWide(SigV4SignerDefaults defaults) {
        return defaults != null
               && defaults.service() != null
               && Objects.equals(intermediateModel.getMetadata().getServiceName(), defaults.service());
    }

    public Map<List<String>, AuthSchemeCodegenMetadata> operationsToNonStandardSigv4Metadata() {
        Map<List<String>, AuthSchemeCodegenMetadata> result =
            operationsToMetadata()
                .entrySet()
                .stream()
                .filter(kvp -> containsNonStandardSigV4(kvp.getValue()))
                .map(kvp -> new AbstractMap.SimpleEntry<>(
                    kvp.getKey(),
                    findNonStandardSigV4(kvp.getValue())))
                .collect(
                    Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue,
                                     (a, b) -> b,
                                     LinkedHashMap::new));
        return result;
    }

    private boolean containsNonStandardSigV4(List<AuthSchemeCodegenMetadata> options) {
        return findNonStandardSigV4(options) != null;
    }

    private AuthSchemeCodegenMetadata findNonStandardSigV4(List<AuthSchemeCodegenMetadata> options) {
        Map<String, Object> defaultSigv4Properties =
            AuthSchemeCodegenMetadata.constantProperties(AuthSchemeCodegenMetadata.SIGV4);
        for (AuthSchemeCodegenMetadata metadata : options) {
            if (metadata.authSchemeClass() != AwsV4AuthScheme.class) {
                continue;
            }
            Map<String, Object> sigv4Properties = AuthSchemeCodegenMetadata.constantProperties(metadata);
            if (defaultSigv4Properties.equals(sigv4Properties)) {
                return null;
            }
            List<AuthSchemeCodegenMetadata.SignerPropertyValueProvider> properties =
                metadata
                    .properties()
                    .stream()
                    .filter(AuthSchemeSpecUtils::isNonDefaultSigv4Property)
                    .collect(Collectors.toList());
            if (!properties.isEmpty()) {
                return metadata.toBuilder().properties(properties).build();
            }
            return null;
        }
        return null;
    }

    private static boolean isNonDefaultSigv4Property(AuthSchemeCodegenMetadata.SignerPropertyValueProvider provider) {
        switch (provider.fieldName()) {
            case "SERVICE_SIGNING_NAME":
            case "REGION_NAME":
            case "DOUBLE_URL_ENCODE":
                return false;
            default:
                return true;
        }
    }

    private List<AuthSchemeCodegenMetadata> authTypeToCodegenMetadata(List<AuthType> authTypes) {
        return authTypes.stream().map(AuthSchemeCodegenMetadata::fromAuthType).collect(Collectors.toList());
    }

    public List<AuthType> serviceDefaultAuthTypes() {
        List<AuthType> modeled = intermediateModel.getMetadata().getAuth();
        if (!modeled.isEmpty()) {
            return modeled;
        }
        return Collections.singletonList(intermediateModel.getMetadata().getAuthType());
    }

    public Set<Class<?>> allServiceConcreteAuthSchemeClasses() {
        Set<Class<?>> result = operationsToMetadata()
            .values()
            .stream()
            .flatMap(Collection::stream)
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
