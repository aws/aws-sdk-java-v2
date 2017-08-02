/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.naming;

import static software.amazon.awssdk.codegen.internal.Constants.AUTHORIZER_NAME_PREFIX;
import static software.amazon.awssdk.codegen.internal.Constants.EXCEPTION_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constants.FAULT_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constants.REQUEST_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constants.RESPONSE_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constants.VARIABLE_NAME_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Utils.capitialize;
import static software.amazon.awssdk.codegen.internal.Utils.unCapitialize;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.Output;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Default implementation of naming strategy respecting customizations supplied by {@link
 * CustomizationConfig}.
 */
public class DefaultNamingStrategy implements NamingStrategy {

    private static final Set<String> RESERVED_KEYWORDS = new HashSet<String>() {
        {
            add("return");
            add("public");
            add("private");
            add("class");
            add("static");
            add("protected");
            add("string");
            add("boolean");
            add("integer");
            add("int");
            add("char");
            add("null");
            add("double");
            add("object");
            add("short");
            add("long");
            add("float");
            add("byte");
            add("bigDecimal");
            add("bigInteger");
            add("protected");
            add("inputStream");
            add("bytebuffer");
            add("date");
            add("list");
            add("map");
        }
    };

    private final ServiceModel serviceModel;
    private final CustomizationConfig customizationConfig;

    public DefaultNamingStrategy(ServiceModel serviceModel,
                                 CustomizationConfig customizationConfig) {
        this.serviceModel = serviceModel;
        this.customizationConfig = customizationConfig;
    }

    private static boolean isJavaKeyword(String word) {
        return RESERVED_KEYWORDS.contains(word) ||
               RESERVED_KEYWORDS.contains(StringUtils.lowerCase(word));
    }

    @Override
    public String getExceptionName(String errorShapeName) {

        if (errorShapeName.endsWith(FAULT_CLASS_SUFFIX)) {
            return capitialize(errorShapeName.substring(0, errorShapeName.length() -
                                                           FAULT_CLASS_SUFFIX.length()) +
                               EXCEPTION_CLASS_SUFFIX);
        } else if (errorShapeName.endsWith(EXCEPTION_CLASS_SUFFIX)) {
            return capitialize(errorShapeName);
        }
        return capitialize(errorShapeName + EXCEPTION_CLASS_SUFFIX);
    }

    @Override
    public String getRequestClassName(String operationName) {
        return capitialize(operationName + REQUEST_CLASS_SUFFIX);
    }

    @Override
    public String getResponseClassName(String operationName) {
        if (customizationConfig.useModeledOutputShapeNames()) {
            final Output operationOutput = serviceModel.getOperation(operationName).getOutput();
            if (operationOutput != null) {
                return operationOutput.getShape();
            }
        }
        return capitialize(operationName + RESPONSE_CLASS_SUFFIX);
    }

    @Override
    public String getVariableName(String name) {
        if (isJavaKeyword(name)) {
            return unCapitialize(name + VARIABLE_NAME_SUFFIX);
        } else {
            return unCapitialize(name);
        }
    }

    @Override
    public String getEnumValueName(String enumValue) {
        StringBuilder builder = new StringBuilder();

        String sanitizedEnumValue = enumValue.replace("::", ":").replace("/", "").replace("(", "")
                                             .replace(")", "");

        for (String part : sanitizedEnumValue.split("[ -.:]")) {
            if (part.length() > 1) {
                builder.append(StringUtils.upperCase(part.substring(0, 1)))
                       .append(part.substring(1));
            } else {
                builder.append(StringUtils.upperCase(part));
            }
        }

        return builder.toString();
    }

    @Override
    public String getJavaClassName(String shapeName) {
        return Arrays.stream(shapeName.split("[._-]|\\W")).map(Utils::capitialize).collect(Collectors.joining());
    }

    @Override
    public String getAuthorizerClassName(String shapeName) {
        String converted = getJavaClassName(shapeName);
        if (converted.length() > 0 && !Character.isLetter(converted.charAt(0))) {
            return AUTHORIZER_NAME_PREFIX + converted;
        }
        return converted;
    }

    @Override
    public String getFluentGetterMethodName(String memberName) {
        return Utils.unCapitialize(memberName);
    }

    @Override
    public String getBeanStyleGetterMethodName(String memberName) {
        return String.format("get%s", Utils.capitialize(memberName));

    }

    @Override
    public String getSetterMethodName(String memberName) {
        return Utils.unCapitialize(memberName);
    }

    @Override
    public String getBeanStyleSetterMethodName(String memberName) {
        return String.format("set%s", Utils.capitialize(memberName));
    }

    @Override
    public String getFluentSetterMethodName(String memberName) {
        return Utils.unCapitialize(memberName);
    }
}
