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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.http.auth.spi.HttpAuthOption;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

public final class AuthSchemeSpecUtils {
    private final IntermediateModel intermediateModel;

    public AuthSchemeSpecUtils(IntermediateModel intermediateModel) {
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

    public String paramMethodName(String param) {
        return Utils.unCapitalize(CodegenNamingUtils.pascalCase(param));
    }

    public boolean generateEndpointBasedParams() {
        return "S3".equals(intermediateModel.getMetadata().getServiceName());
    }

    public boolean includeParam(ParameterModel model, String name) {
        if (usesSigV4()) {
            String methodName = paramMethodName(name);
            return !"region".equals(methodName);
        }
        return true;
    }

    public MethodSpec.Builder endpointParamAccessorSignature(ParameterModel model, String name) {
        String methodName = Utils.unCapitalize(CodegenNamingUtils.pascalCase(name));
        TypeName typeName = EndpointRulesSpecUtils.parameterType(model);
        MethodSpec.Builder spec = MethodSpec.methodBuilder(methodName)
                                            .addModifiers(Modifier.PUBLIC);
        String docs = model.getDocumentation();
        if (docs != null) {
            spec.addJavadoc(docs);
        }
        return spec.returns(typeName);
    }
}
