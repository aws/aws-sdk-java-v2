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
import java.util.HashMap;
import java.util.HashSet;
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

    public ClassName providerDefaultImplName() {
        return ClassName.get(internalPackage(), "Default" + providerInterfaceName().simpleName());
    }

    public TypeName resolverReturnType() {
        return ParameterizedTypeName.get(List.class, AuthSchemeOption.class);
    }

    public boolean usesSigV4() {
        return AuthUtils.usesAwsAuth(intermediateModel);
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
        Map<List<AuthType>, List<String>> authSchemes2Operations =
            intermediateModel.getOperations()
                             .entrySet()
                             .stream()
                             .filter(kvp -> !kvp.getValue().getAuth().isEmpty())
                             .collect(Collectors.groupingBy(kvp -> kvp.getValue().getAuth(),
                                                            Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        Map<List<String>, List<AuthType>> result = new HashMap<>();
        authSchemes2Operations.forEach((key, value) -> result.put(value, key));
        List<AuthType> serviceDefaults;
        if (intermediateModel.getMetadata().getAuth().isEmpty()) {
            serviceDefaults = Arrays.asList(intermediateModel.getMetadata().getAuthType());
        } else {
            serviceDefaults = intermediateModel.getMetadata().getAuth();
        }

        // Get the list of operations that share the same auth schemes as the system defaults and remove it from the result. We
        // will take care of all of these in the fallback `default` case.
        List<String> operationsWithDefaults = authSchemes2Operations.remove(serviceDefaults);
        result.remove(operationsWithDefaults);
        result.put(Collections.emptyList(), serviceDefaults);
        return result;
    }
}
