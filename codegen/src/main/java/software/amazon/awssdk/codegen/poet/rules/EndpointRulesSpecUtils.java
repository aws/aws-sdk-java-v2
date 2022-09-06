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

package software.amazon.awssdk.codegen.poet.rules;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import java.util.Locale;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.regions.Region;

public class EndpointRulesSpecUtils {
    private final IntermediateModel intermediateModel;

    public EndpointRulesSpecUtils(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
    }

    public String basePackage() {
        return intermediateModel.getMetadata().getFullEndpointRulesPackageName();
    }

    public ClassName parametersClassName() {
        return ClassName.get(basePackage(), intermediateModel.getMetadata().getServiceName() + "EndpointParams");
    }

    public ClassName providerInterfaceName() {
        return ClassName.get(basePackage(), intermediateModel.getMetadata().getServiceName() + "EndpointProvider");
    }

    public ClassName providerDefaultImplName() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + providerInterfaceName().simpleName());
    }

    public String paramSetterName(String param) {
        return Utils.unCapitalize(param);
    }

    public TypeName toJavaType(String type) {
        switch (type.toLowerCase(Locale.ENGLISH)) {
            case "boolean":
                return TypeName.get(Boolean.class);
            case "string":
                return TypeName.get(String.class);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    public CodeBlock valueCreationCode(String type, CodeBlock param) {
        String methodName;
        switch (type.toLowerCase(Locale.ENGLISH)) {
            case "boolean":
                methodName = "fromBool";
                break;
            case "string":
                methodName = "fromStr";
                break;
            default:
                throw new RuntimeException("Don't know how to create a Value instance from type " + type);
        }

        return CodeBlock.builder()
                        .add("$T.$N($L)", Value.class, methodName, param)
                        .build();
    }

    public TypeName parameterType(ParameterModel param) {
        if (param.getBuiltIn() == null) {
            return toJavaType(param.getType());
        }

        if ("aws::region".equals(param.getBuiltIn().toLowerCase(Locale.ENGLISH))) {
            return ClassName.get(Region.class);
        }
        return ClassName.get(Boolean.class);
    }
}
