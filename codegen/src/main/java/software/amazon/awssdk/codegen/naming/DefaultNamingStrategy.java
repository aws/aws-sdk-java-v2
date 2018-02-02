/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Default implementation of naming strategy respecting.
 */
public class DefaultNamingStrategy implements NamingStrategy {

    private static Logger log = Logger.loggerFor(DefaultNamingStrategy.class);

    private static final Set<String> RESERVED_KEYWORDS;

    static {
        Set<String> keywords = new HashSet<>();
        Collections.addAll(keywords,
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
                "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for",
                "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package",
                "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
                "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "true",
                "null", "false", "const", "goto");
        RESERVED_KEYWORDS = Collections.unmodifiableSet(keywords);
    }

    private final ServiceModel serviceModel;

    public DefaultNamingStrategy(ServiceModel serviceModel,
                                 CustomizationConfig customizationConfig) {
        this.serviceModel = serviceModel;
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
        String result = enumValue;

        // Special cases
        result = result.replaceAll("textORcsv", "TEXT_OR_CSV");

        // Convert non-underscore word boundaries into underscore word boundaries
        result = result.replaceAll("[:/()-. ]+", "_"); // acm-success -> acm_success

        // If the number had a standalone v in front of it, separate it out (version).
        result = result.replaceAll("([^a-z]{2,})v([0-9]+)", "$1_v$2_") // TESTv4 -> TEST_v4_
                       .replaceAll("([^A-Z]{2,})V([0-9]+)", "$1_V$2_"); // TestV4 -> Test_V4_

        // Add an underscore between camelCased words
        result = result.replaceAll("([a-z])([A-Z][a-zA-Z])", "$1_$2"); // AcmSuccess -> Acm_Success

        // Add an underscore after acronyms
        result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2"); // ACMSuccess -> ACM_Success

        // Add an underscore after a number in the middle of a word
        result = result.replaceAll("([0-9])([a-zA-Z])", "$1_$2"); // s3ec2 -> s3_ec2

        // Remove extra underscores - multiple consecutive ones or those and the beginning/end of words
        result = result.replaceAll("_+", "_") // Foo__Bar -> Foo_Bar
                       .replaceAll("^_*([^_].*[^_])_*$", "$1"); // _Foo_ -> Foo

        // Convert all lower-case words
        result = StringUtils.upperCase(result);

        if (!result.matches("^[A-Z][A-Z0-9_]*$")) {
            String attempt = result;
            log.warn(() -> "Invalid enum member generated for input '" + enumValue + "'. Best attempt: '" + attempt + "' If this "
                           + "enum is not customized out, the build will fail.");
        }

        return result;
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
    public String getFluentGetterMethodName(String memberName, Shape shape) {
        String getterMethodName = Utils.unCapitialize(memberName);

        if (Utils.isOrContainsEnumShape(shape, serviceModel.getShapes())) {
            getterMethodName += "String";

            if (Utils.isListShape(shape) || Utils.isMapShape(shape)) {
                getterMethodName += "s";
            }
        }

        return getterMethodName;
    }

    @Override
    public String getFluentEnumGetterMethodName(String memberName, Shape shape) {
        if (!Utils.isOrContainsEnumShape(shape, serviceModel.getShapes())) {
            return null;
        }

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
