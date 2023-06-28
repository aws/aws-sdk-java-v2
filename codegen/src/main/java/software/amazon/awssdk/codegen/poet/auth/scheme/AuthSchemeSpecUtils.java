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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;

public final class AuthSchemeSpecUtils {
    private final IntermediateModel intermediateModel;
    private final Set<String> allowedEndpointAuthSchemeParams;

    public AuthSchemeSpecUtils(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.allowedEndpointAuthSchemeParams = Collections.unmodifiableSet(
            new HashSet<>(intermediateModel.getCustomizationConfig().getAllowedEndpointAuthSchemeParams()));
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

    public ClassName internalModeledAuthSchemeProviderName() {
        return ClassName.get(internalPackage(), "InternalModeled" + providerInterfaceName().simpleName());
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
        return allowedEndpointAuthSchemeParams.contains(name);
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

        List<AuthType> serviceDefaults;
        if (intermediateModel.getMetadata().getAuth().isEmpty()) {
            serviceDefaults = Arrays.asList(intermediateModel.getMetadata().getAuthType());
        } else {
            serviceDefaults = intermediateModel.getMetadata().getAuth();
        }

        // Get the list of operations that share the same auth schemes as the system defaults and remove it from the result. We
        // will take care of all of these in the fallback `default` case.
        List<String> operationsWithDefaults = authSchemesToOperations.remove(serviceDefaults);
        operationsToAuthType.remove(operationsWithDefaults);
        operationsToAuthType.put(Collections.emptyList(), serviceDefaults);
        return operationsToAuthType;
    }
}
