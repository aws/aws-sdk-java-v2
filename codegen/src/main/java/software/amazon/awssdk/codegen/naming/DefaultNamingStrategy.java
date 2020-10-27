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

package software.amazon.awssdk.codegen.naming;

import static java.util.stream.Collectors.joining;
import static software.amazon.awssdk.codegen.internal.Constant.CONFLICTING_NAME_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.EXCEPTION_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.FAULT_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.REQUEST_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.RESPONSE_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Utils.unCapitalize;
import static software.amazon.awssdk.utils.internal.CodegenNamingUtils.pascalCase;
import static software.amazon.awssdk.utils.internal.CodegenNamingUtils.splitOnWordBoundaries;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.internal.Constant;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.UnderscoresInNameBehavior;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of naming strategy respecting.
 */
public class DefaultNamingStrategy implements NamingStrategy {

    private static final Logger log = Logger.loggerFor(DefaultNamingStrategy.class);

    private static final Pattern VALID_IDENTIFIER_NAME =
        Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

    private static final String COLLISION_DISAMBIGUATION_PREFIX = "Default";

    private static final Set<String> RESERVED_KEYWORDS;

    private static final Set<String> RESERVED_EXCEPTION_METHOD_NAMES;

    private static final Set<Object> RESERVED_STRUCTURE_METHOD_NAMES;

    static {
        Set<String> reservedJavaKeywords = new HashSet<>();
        Collections.addAll(reservedJavaKeywords,
                           "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
                           "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
                           "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
                           "interface", "long", "native", "new", "null", "package", "private", "protected", "public",
                           "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
                           "throws", "transient", "true", "try", "void", "volatile", "while");
        RESERVED_KEYWORDS = Collections.unmodifiableSet(reservedJavaKeywords);


        Set<String> reservedJavaMethodNames = new HashSet<>();
        Collections.addAll(reservedJavaMethodNames,
                           "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait");

        Set<String> reserveJavaPojoMethodNames = new HashSet<>(reservedJavaMethodNames);
        Collections.addAll(reserveJavaPojoMethodNames,
                           "builder", "sdkFields", "toBuilder");

        Set<String> reservedExceptionMethodNames = new HashSet<>(reserveJavaPojoMethodNames);
        Collections.addAll(reservedExceptionMethodNames,
                           "awsErrorDetails", "cause", "fillInStackTrace", "getCause", "getLocalizedMessage",
                           "getMessage", "getStackTrace", "getSuppressed", "isClockSkewException", "isThrottlingException",
                           "printStackTrace", "requestId", "retryable", "serializableBuilderClass", "statusCode");
        RESERVED_EXCEPTION_METHOD_NAMES = Collections.unmodifiableSet(reservedExceptionMethodNames);

        Set<String> reservedStructureMethodNames = new HashSet<>(reserveJavaPojoMethodNames);
        Collections.addAll(reservedStructureMethodNames,
                           "overrideConfiguration", "sdkHttpResponse");
        RESERVED_STRUCTURE_METHOD_NAMES = Collections.unmodifiableSet(reservedStructureMethodNames);
    }

    private final ServiceModel serviceModel;
    private final CustomizationConfig customizationConfig;

    public DefaultNamingStrategy(ServiceModel serviceModel,
                                 CustomizationConfig customizationConfig) {
        this.serviceModel = serviceModel;
        this.customizationConfig = customizationConfig == null ? CustomizationConfig.create() : customizationConfig;
    }

    private static boolean isJavaKeyword(String word) {
        return RESERVED_KEYWORDS.contains(word) ||
               RESERVED_KEYWORDS.contains(StringUtils.lowerCase(word));
    }

    @Override
    public String getServiceName() {
        String baseName = Stream.of(serviceModel.getMetadata().getServiceId())
                                .filter(Objects::nonNull)
                                .filter(s -> !s.trim().isEmpty())
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("ServiceId is missing in the c2j model."));

        baseName = pascalCase(baseName);

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
    public String getWaitersPackageName(String serviceName) {
        return getCustomizedPackageName(concatServiceNameIfShareModel(serviceName), Constant.PACKAGE_NAME_WAITERS_PATTERN);
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

    private String screamCase(String word) {
        return Stream.of(splitOnWordBoundaries(word)).map(s -> s.toUpperCase(Locale.US)).collect(joining("_"));
    }

    private String getCustomizedPackageName(String serviceName, String defaultPattern) {
        return String.format(defaultPattern, StringUtils.lowerCase(serviceName));
    }

    @Override
    public String getExceptionName(String errorShapeName) {
        String baseName;
        if (errorShapeName.endsWith(FAULT_CLASS_SUFFIX)) {
            baseName = pascalCase(errorShapeName.substring(0, errorShapeName.length() - FAULT_CLASS_SUFFIX.length())) +
                              EXCEPTION_CLASS_SUFFIX;
        } else if (errorShapeName.endsWith(EXCEPTION_CLASS_SUFFIX)) {
            baseName = pascalCase(errorShapeName);
        } else {
            baseName = pascalCase(errorShapeName) + EXCEPTION_CLASS_SUFFIX;
        }
        if (baseName.equals(getServiceName() + EXCEPTION_CLASS_SUFFIX)) {
            return COLLISION_DISAMBIGUATION_PREFIX + baseName;
        }
        return baseName;
    }

    @Override
    public String getRequestClassName(String operationName) {
        String baseName = pascalCase(operationName) + REQUEST_CLASS_SUFFIX;
        if (!operationName.equals(getServiceName())) {
            return baseName;
        }

        return COLLISION_DISAMBIGUATION_PREFIX + baseName;
    }

    @Override
    public String getResponseClassName(String operationName) {
        String baseName = pascalCase(operationName) + RESPONSE_CLASS_SUFFIX;
        if (!operationName.equals(getServiceName())) {
            return baseName;
        }

        return COLLISION_DISAMBIGUATION_PREFIX + baseName;
    }

    @Override
    public String getVariableName(String name) {
        // Exclude keywords because they will not compile, and exclude reserved method names because they're frequently
        // used for local variable names.
        if (isJavaKeyword(name) ||
            RESERVED_STRUCTURE_METHOD_NAMES.contains(name) ||
            RESERVED_EXCEPTION_METHOD_NAMES.contains(name)) {
            return unCapitalize(name + CONFLICTING_NAME_SUFFIX);
        }

        return unCapitalize(name);
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
    public String getShapeClassName(String shapeName) {
        return Utils.capitalize(shapeName);
    }

    @Override
    public String getFluentGetterMethodName(String memberName, Shape parentShape, Shape shape) {
        String getterMethodName = Utils.unCapitalize(memberName);

        getterMethodName = rewriteInvalidMemberName(getterMethodName, parentShape);

        if (Utils.isOrContainsEnumShape(shape, serviceModel.getShapes())) {
            getterMethodName += "AsString";

            if (Utils.isListShape(shape) || Utils.isMapShape(shape)) {
                getterMethodName += "s";
            }
        }

        return getterMethodName;
    }

    @Override
    public String getFluentEnumGetterMethodName(String memberName, Shape parentShape, Shape shape) {
        if (!Utils.isOrContainsEnumShape(shape, serviceModel.getShapes())) {
            return null;
        }

        String getterMethodName = Utils.unCapitalize(memberName);
        getterMethodName = rewriteInvalidMemberName(getterMethodName, parentShape);
        return getterMethodName;
    }

    @Override
    public String getExistenceCheckMethodName(String memberName, Shape parentShape) {
        String existenceCheckMethodName = Utils.unCapitalize(memberName);
        existenceCheckMethodName = rewriteInvalidMemberName(existenceCheckMethodName, parentShape);
        return String.format("has%s", Utils.capitalize(existenceCheckMethodName));
    }

    @Override
    public String getBeanStyleGetterMethodName(String memberName, Shape parentShape, Shape c2jShape) {
        String fluentGetterMethodName;
        if (Utils.isOrContainsEnumShape(c2jShape, serviceModel.getShapes())) {
            // Use the enum (modeled) name for bean-style getters
            fluentGetterMethodName = getFluentEnumGetterMethodName(memberName, parentShape, c2jShape);
        } else {
            fluentGetterMethodName = getFluentGetterMethodName(memberName, parentShape, c2jShape);
        }
        return String.format("get%s", Utils.capitalize(fluentGetterMethodName));
    }

    @Override
    public String getBeanStyleSetterMethodName(String memberName, Shape parentShape, Shape c2jShape) {
        String beanStyleGetter = getBeanStyleGetterMethodName(memberName, parentShape, c2jShape);
        return String.format("set%s", beanStyleGetter.substring("get".length()));
    }

    @Override
    public String getFluentSetterMethodName(String memberName, Shape parentShape, Shape shape) {
        String setterMethodName = Utils.unCapitalize(memberName);

        setterMethodName = rewriteInvalidMemberName(setterMethodName, parentShape);

        if (Utils.isOrContainsEnumShape(shape, serviceModel.getShapes()) &&
            (Utils.isListShape(shape) || Utils.isMapShape(shape))) {

            setterMethodName += "WithStrings";
        }

        return setterMethodName;
    }

    @Override
    public String getFluentEnumSetterMethodName(String memberName, Shape parentShape, Shape shape) {
        if (!Utils.isOrContainsEnumShape(shape, serviceModel.getShapes())) {
            return null;
        }

        String setterMethodName = Utils.unCapitalize(memberName);
        setterMethodName = rewriteInvalidMemberName(setterMethodName, parentShape);
        return setterMethodName;
    }

    @Override
    public String getSdkFieldFieldName(MemberModel memberModel) {
        return screamCase(memberModel.getName()) + "_FIELD";
    }

    private String rewriteInvalidMemberName(String memberName, Shape parentShape) {
        if (isJavaKeyword(memberName) || isDisallowedNameForShape(memberName, parentShape)) {
            return Utils.unCapitalize(memberName + CONFLICTING_NAME_SUFFIX);
        }

        return memberName;
    }

    private boolean isDisallowedNameForShape(String name, Shape parentShape) {
        if (parentShape.isException()) {
            return RESERVED_EXCEPTION_METHOD_NAMES.contains(name);
        } else {
            return RESERVED_STRUCTURE_METHOD_NAMES.contains(name);
        }
    }

    @Override
    public void validateCustomerVisibleNaming(IntermediateModel trimmedModel) {
        Metadata metadata = trimmedModel.getMetadata();
        validateCustomerVisibleName(metadata.getSyncInterface(), "metadata-derived interface name");
        validateCustomerVisibleName(metadata.getSyncBuilderInterface(), "metadata-derived builder interface name");
        validateCustomerVisibleName(metadata.getAsyncInterface(), "metadata-derived async interface name");
        validateCustomerVisibleName(metadata.getAsyncBuilderInterface(), "metadata-derived async builder interface name");
        validateCustomerVisibleName(metadata.getBaseBuilderInterface(), "metadata-derived builder interface name");
        validateCustomerVisibleName(metadata.getBaseExceptionName(), "metadata-derived exception name");
        validateCustomerVisibleName(metadata.getBaseRequestName(), "metadata-derived request name");
        validateCustomerVisibleName(metadata.getBaseResponseName(), "metadata-derived response name");

        trimmedModel.getOperations().values().forEach(operation -> {
            validateCustomerVisibleName(operation.getOperationName(), "operations");
        });

        trimmedModel.getWaiters().forEach((name, waiter) -> {
            validateCustomerVisibleName(name, "waiters");
        });

        trimmedModel.getShapes().values().forEach(shape -> {
            String shapeName = shape.getShapeName();
            validateCustomerVisibleName(shapeName, "shapes");
            shape.getMembers().forEach(member -> {
                validateCustomerVisibleName(member.getFluentGetterMethodName(), shapeName + " shape");
                validateCustomerVisibleName(member.getFluentSetterMethodName(), shapeName + " shape");
                validateCustomerVisibleName(member.getFluentEnumGetterMethodName(), shapeName + " shape");
                validateCustomerVisibleName(member.getFluentEnumSetterMethodName(), shapeName + " shape");
                validateCustomerVisibleName(member.getExistenceCheckMethodName(), shapeName + " shape");
                validateCustomerVisibleName(member.getBeanStyleGetterMethodName(), shapeName + " shape");
                validateCustomerVisibleName(member.getBeanStyleSetterMethodName(), shapeName + " shape");
                validateCustomerVisibleName(member.getEnumType(), shapeName + " shape");
            });
        });
    }

    private void validateCustomerVisibleName(String name, String location) {
        if (name == null) {
            return;
        }

        if (name.contains("_")) {
            UnderscoresInNameBehavior behavior = customizationConfig.getUnderscoresInNameBehavior();

            String supportedBehaviors = Arrays.toString(UnderscoresInNameBehavior.values());
            Validate.notNull(behavior,
                             "Encountered a name or identifier that the customer will see (%s in the %s) with an underscore. "
                             + "This isn't idiomatic in Java. Please either remove the underscores or apply the "
                             + "'underscoresInNameBehavior' customization for this service (Supported "
                             + "'underscoresInNameBehavior' values: %s).", name, location, supportedBehaviors);
            Validate.isTrue(behavior == UnderscoresInNameBehavior.ALLOW,
                            "Unsupported underscoresInShapeNameBehavior: %s. Supported values: %s", behavior, supportedBehaviors);
        }

        Validate.isTrue(VALID_IDENTIFIER_NAME.matcher(name).matches(),
                        "Encountered a name or identifier that is invalid within Java (%s in %s). Please remove invalid "
                        + "characters.", name, location);
    }
}
