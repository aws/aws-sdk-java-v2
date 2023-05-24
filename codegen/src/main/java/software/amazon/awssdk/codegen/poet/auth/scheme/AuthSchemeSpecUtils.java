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
import java.util.List;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.http.auth.spi.HttpAuthOption;

final class AuthSchemeSpecUtils {
    private final IntermediateModel intermediateModel;

    AuthSchemeSpecUtils(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
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

    public ClassName providerInterfaceName() {
        return ClassName.get(basePackage(), intermediateModel.getMetadata().getServiceName() + "AuthSchemeProvider");
    }

    public ClassName providerDefaultImplName() {
        return ClassName.get(internalPackage(), "Default" + providerInterfaceName().simpleName());
    }

    public TypeName resolverReturnType() {
        return ParameterizedTypeName.get(List.class, HttpAuthOption.class);
    }

    public boolean usesSigV4() {
        return AuthUtils.usesAwsAuth(intermediateModel);
    }

}
