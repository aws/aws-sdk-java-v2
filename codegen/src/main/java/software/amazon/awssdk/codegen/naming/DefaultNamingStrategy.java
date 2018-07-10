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

import static java.util.stream.Collectors.joining;
import static software.amazon.awssdk.codegen.internal.Constant.AUTHORIZER_NAME_PREFIX;
import static software.amazon.awssdk.codegen.internal.Constant.EXCEPTION_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.FAULT_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.REQUEST_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.RESPONSE_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.VARIABLE_NAME_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Utils.unCapitalize;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.codegen.internal.Constant;
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
    @ReviewBeforeRelease("Always use service ID as the base.")
    public String getServiceName() {
        String baseName = Stream.of(serviceModel.getMetadata().getServiceId(),
                                    serviceModel.getMetadata().getServiceAbbreviation(),
                                    serviceModel.getMetadata().getServiceFullName())
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("No non-null service name descriptor."));

        baseName = pascalCase(splitOnWordBoundaries(baseName));

        // Special cases
        baseName = Utils.removeLeading(baseName, "Amazon");
        baseName = Utils.removeLeading(baseName, "Aws");
        baseName = Utils.removeTrailing(baseName, "Service");

        return baseName;
    }

    @Override
    public String getClientPackageName(String serviceName) {
        return getCustomizedPackageName(concatServiceNameIfShareModel(serviceName),
                                        Constant.PACKAGE_NAME_CLIENT_PATTERN);
    }

    @Override
    public String getModelPackageName(String serviceName) {
        // Share model package classes if we are sharing models.
        if (customizationConfig.getShareModelConfig() != null
            && customizationConfig.getShareModelConfig().getShareModelWith() != null) {
            serviceName = customizationConfig.getShareModelConfig().getShareModelWith();
        }
        return getCustomizedPackageName(serviceName,
                                        Constant.PACKAGE_NAME_MODEL_PATTERN);
    }

    @Override
    public String getTransformPackageName(String serviceName) {
        // Share transform package classes if we are sharing models.
        if (customizationConfig.getShareModelConfig() != null
            && customizationConfig.getShareModelConfig().getShareModelWith() != null) {
            serviceName = customizationConfig.getShareModelConfig().getShareModelWith();
        }
        return getCustomizedPackageName(serviceName,
                                        Constant.PACKAGE_NAME_TRANSFORM_PATTERN);
    }

    @Override
    public String getRequestTransformPackageName(String serviceName) {
        return getCustomizedPackageName(concatServiceNameIfShareModel(serviceName),
                                        Constant.PACKAGE_NAME_TRANSFORM_PATTERN);
    }

    @Override
    public String getPaginatorsPackageName(String serviceName) {
        return getCustomizedPackageName(concatServiceNameIfShareModel(serviceName), Constant.PACKAGE_NAME_PAGINATORS_PATTERN);
    }

    @Override
    public String getSmokeTestPackageName(String serviceName) {

        return getCustomizedPackageName(concatServiceNameIfShareModel(serviceName),
                                        Constant.PACKAGE_NAME_SMOKE_TEST_PATTERN);
    }

    /**
     * If the service is sharing models with other services, we need to concatenate its customized package name
     * if provided or service name with the shared service name.
     */
    private String concatServiceNameIfShareModel(String serviceName) {

        if (customizationConfig.getShareModelConfig() != null) {
            return customizationConfig.getShareModelConfig().getShareModelWith() + "." +
                   Optional.ofNullable(customizationConfig.getShareModelConfig().getPackageName()).orElse(serviceName);
        }

        return serviceName;
    }

    private String pascalCase(String word) {
        return Stream.of(splitOnWordBoundaries(word)).map(StringUtils::lowerCase).map(Utils::capitalize).collect(joining());
    }

    private String pascalCase(String... words) {
        return Stream.of(words).map(StringUtils::lowerCase).map(Utils::capitalize).collect(joining());
    }

    private String getCustomizedPackageName(String serviceName, String defaultPattern) {
        return String.format(defaultPattern, StringUtils.lowerCase(serviceName));
    }

    @Override
    public String getExceptionName(String errorShapeName) {
        if (errorShapeName.endsWith(FAULT_CLASS_SUFFIX)) {
            return pascalCase(errorShapeName.substring(0, errorShapeName.length() - FAULT_CLASS_SUFFIX.length())) +
                              EXCEPTION_CLASS_SUFFIX;
        } else if (errorShapeName.endsWith(EXCEPTION_CLASS_SUFFIX)) {
            return pascalCase(errorShapeName);
        }
        return pascalCase(errorShapeName) + EXCEPTION_CLASS_SUFFIX;
    }

    @Override
    public String getRequestClassName(String operationName) {
        return pascalCase(operationName) + REQUEST_CLASS_SUFFIX;
    }

    @Override
    public String getResponseClassName(String operationName) {
        return pascalCase(operationName) + RESPONSE_CLASS_SUFFIX;
    }

    @Override
    public String getVariableName(String name) {
        if (isJavaKeyword(name)) {
            return unCapitalize(name + VARIABLE_NAME_SUFFIX);
        } else {
            return unCapitalize(name);
        }
    }

    @Override
    public String getEnumValueName(String enumValue) {
        String result = enumValue;

        // Special cases
        result = result.replaceAll("textORcsv", "TEXT_OR_CSV");

        // Split into words
        result = String.join("_", splitOnWordBoundaries(result));

        // Enums should be upper-case
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
        return Arrays.stream(shapeName.split("[._-]|\\W")).map(Utils::capitalize).collect(Collectors.joining());
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
        String getterMethodName = Utils.unCapitalize(memberName);

        if (Utils.isOrContainsEnumShape(shape, serviceModel.getShapes())) {
            getterMethodName += "AsString";

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

        return Utils.unCapitalize(memberName);
    }

    @Override
    public String getBeanStyleGetterMethodName(String memberName) {
        return String.format("get%s", Utils.capitalize(memberName));
    }

    @Override
    public String getSetterMethodName(String memberName) {
        return Utils.unCapitalize(memberName);
    }

    @Override
    public String getBeanStyleSetterMethodName(String memberName) {
        return String.format("set%s", Utils.capitalize(memberName));
    }

    @Override
    public String getFluentSetterMethodName(String memberName, Shape shape) {
        String setterMethodName = Utils.unCapitalize(memberName);

        if (Utils.isOrContainsEnumShape(shape, serviceModel.getShapes()) &&
            (Utils.isListShape(shape) || Utils.isMapShape(shape))) {

            setterMethodName += "WithStrings";
        }

        return setterMethodName;
    }

    @Override
    public String getFluentEnumSetterMethodName(String memberName, Shape shape) {
        if (!Utils.isOrContainsEnumShape(shape, serviceModel.getShapes())) {
            return null;
        }

        return Utils.unCapitalize(memberName);
    }

    private String[] splitOnWordBoundaries(String toSplit) {
        String result = toSplit;

        // All non-alphanumeric characters are spaces
        result = result.replaceAll("[^A-Za-z0-9]+", " "); // acm-success -> "acm success"

        // If a number has a standalone v in front of it, separate it out (version).
        result = result.replaceAll("([^a-z]{2,})v([0-9]+)", "$1 v$2 ") // TESTv4 -> "TEST v4 "
                       .replaceAll("([^A-Z]{2,})V([0-9]+)", "$1 V$2 "); // TestV4 -> "Test V4 "

        // Add a space between camelCased words
        result = String.join(" ", result.split("(?<=[a-z])(?=[A-Z]([a-zA-Z]|[0-9]))")); // AcmSuccess -> "Acm Success"

        // Add a space after acronyms
        result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2"); // ACMSuccess -> "ACM Success"

        // Add space after a number in the middle of a word
        result = result.replaceAll("([0-9])([a-zA-Z])", "$1 $2"); // s3ec2 -> "s3 ec2"

        // Remove extra spaces - multiple consecutive ones or those and the beginning/end of words
        result = result.replaceAll(" +", " ") // "Foo  Bar" -> "Foo Bar"
                       .trim(); // " Foo " -> Foo

        return result.split(" ");
    }
}
