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

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.internal.Constant;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * {@link NamingStrategy} implementation for Smithy service models. Produces the
 * same names as {@link DefaultNamingStrategy} for equivalent inputs; the only
 * difference is where service-level metadata is sourced from (Smithy
 * {@link ServiceShape} traits instead of C2J {@code ServiceMetadata}).
 */
public class DefaultSmithyNamingStrategy implements NamingStrategy {
    private static final Logger log = Logger.loggerFor(DefaultSmithyNamingStrategy.class);
    private static final String COLLISION_DISAMBIGUATION_PREFIX = "Default";

    private static final Set<String> RESERVED_KEYWORDS;
    private static final Set<String> RESERVED_EXCEPTION_METHOD_NAMES;
    private static final Set<String> RESERVED_STRUCTURE_METHOD_NAMES;

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

        Set<String> reservedJavaPojoMethodNames = new HashSet<>(reservedJavaMethodNames);
        Collections.addAll(reservedJavaPojoMethodNames,
                           "builder", "sdkFields", "toBuilder");

        Set<String> reservedExceptionMethodNames = new HashSet<>(reservedJavaPojoMethodNames);
        Collections.addAll(reservedExceptionMethodNames,
                           "awsErrorDetails", "cause", "fillInStackTrace", "getCause", "getLocalizedMessage",
                           "getMessage", "getStackTrace", "getSuppressed", "isClockSkewException", "isThrottlingException",
                           "printStackTrace", "requestId", "retryable", "serializableBuilderClass", "statusCode");
        RESERVED_EXCEPTION_METHOD_NAMES = Collections.unmodifiableSet(reservedExceptionMethodNames);

        Set<String> reservedStructureMethodNames = new HashSet<>(reservedJavaPojoMethodNames);
        Collections.addAll(reservedStructureMethodNames,
                           "overrideConfiguration", "sdkHttpResponse");
        RESERVED_STRUCTURE_METHOD_NAMES = Collections.unmodifiableSet(reservedStructureMethodNames);
    }

    private final Model smithyModel;
    private final ServiceShape service;
    private final CustomizationConfig customizationConfig;

    public DefaultSmithyNamingStrategy(Model smithyModel, ServiceShape service,
                                       CustomizationConfig customizationConfig) {
        this.smithyModel = smithyModel;
        this.service = service;
        this.customizationConfig = customizationConfig;
    }

    @Override
    public String getServiceName() {
        String baseName = serviceId();
        baseName = pascalCase(baseName);
        baseName = removeRedundantPrefixesAndSuffixes(baseName);
        return baseName;
    }

    @Override
    public String getServiceNameForEnvironmentVariables() {
        String baseName = serviceId();
        baseName = baseName.replace(' ', '_');
        baseName = StringUtils.upperCase(baseName);
        return baseName;
    }

    @Override
    public String getServiceNameForSystemProperties() {
        return getServiceName();
    }

    @Override
    public String getServiceNameForProfileFile() {
        return StringUtils.lowerCase(getServiceNameForEnvironmentVariables());
    }

    @Override
    public String getClientPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_CLIENT_PATTERN);
    }

    @Override
    public String getModelPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_MODEL_PATTERN);
    }

    @Override
    public String getTransformPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_TRANSFORM_PATTERN);
    }

    @Override
    public String getRequestTransformPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_TRANSFORM_PATTERN);
    }

    @Override
    public String getPaginatorsPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_PAGINATORS_PATTERN);
    }

    @Override
    public String getWaitersPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_WAITERS_PATTERN);
    }

    @Override
    public String getEndpointRulesPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_RULES_PATTERN);
    }

    @Override
    public String getAuthSchemePackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_AUTH_SCHEME_PATTERN);
    }

    @Override
    public String getJmesPathPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_JMESPATH_PATTERN);
    }

    @Override
    public String getBatchManagerPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_BATCHMANAGER_PATTERN);
    }

    @Override
    public String getSmokeTestPackageName(String serviceName) {
        return getCustomizedPackageName(serviceName, Constant.PACKAGE_NAME_SMOKE_TEST_PATTERN);
    }

    @Override
    public String getExceptionName(String errorShapeName) {
        String baseName;
        if (errorShapeName.endsWith(FAULT_CLASS_SUFFIX)) {
            baseName = pascalCase(errorShapeName.substring(0, errorShapeName.length() - FAULT_CLASS_SUFFIX.length()))
                       + EXCEPTION_CLASS_SUFFIX;
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
        if (isJavaKeyword(name)
            || RESERVED_STRUCTURE_METHOD_NAMES.contains(name)
            || RESERVED_EXCEPTION_METHOD_NAMES.contains(name)) {
            return unCapitalize(name + CONFLICTING_NAME_SUFFIX);
        }
        return unCapitalize(name);
    }

    @Override
    public String getVariableName(String name, ShapeInfo parentShape) {
        if (isJavaKeyword(name)
            || isDisallowedNameForShape(unCapitalize(name), parentShape)) {
            return unCapitalize(name + CONFLICTING_NAME_SUFFIX);
        }
        return unCapitalize(name);
    }

    @Override
    public String getEnumValueName(String enumValue) {
        String result = enumValue;

        result = result.replace("textORcsv", "TEXT_OR_CSV");

        if (result.matches("^\\d.*")) {
            result = "VALUE_" + result;
        }

        result = String.join("_", splitOnWordBoundaries(result));
        result = StringUtils.upperCase(result);

        if (!result.matches("^[A-Z][A-Z0-9_]*$")) {
            String attempt = result;
            log.warn(() -> "Invalid enum member generated for input '" + enumValue + "'. Best attempt: '" + attempt + "'");
        }

        return result;
    }

    @Override
    public String getShapeClassName(String shapeName) {
        return Utils.capitalize(shapeName);
    }

    @Override
    public String getFluentGetterMethodName(String memberName, ShapeInfo parentShape, ShapeInfo shape) {
        String getterMethodName = unCapitalize(memberName);
        getterMethodName = rewriteInvalidMemberName(getterMethodName, parentShape);

        if (shape.isOrContainsEnum()) {
            getterMethodName += "AsString";
            if (shape.isList() || shape.isMap()) {
                getterMethodName += "s";
            }
        }

        return getterMethodName;
    }

    @Override
    public String getFluentEnumGetterMethodName(String memberName, ShapeInfo parentShape, ShapeInfo shape) {
        if (!shape.isOrContainsEnum()) {
            return null;
        }
        String getterMethodName = unCapitalize(memberName);
        getterMethodName = rewriteInvalidMemberName(getterMethodName, parentShape);
        return getterMethodName;
    }

    @Override
    public String getBeanStyleGetterMethodName(String memberName, ShapeInfo parentShape, ShapeInfo shape) {
        String fluentGetterMethodName;
        if (shape.isOrContainsEnum()) {
            fluentGetterMethodName = getFluentEnumGetterMethodName(memberName, parentShape, shape);
        } else {
            fluentGetterMethodName = getFluentGetterMethodName(memberName, parentShape, shape);
        }
        return String.format("get%s", Utils.capitalize(fluentGetterMethodName));
    }

    @Override
    public String getBeanStyleSetterMethodName(String memberName, ShapeInfo parentShape, ShapeInfo shape) {
        String beanStyleGetter = getBeanStyleGetterMethodName(memberName, parentShape, shape);
        return String.format("set%s", beanStyleGetter.substring("get".length()));
    }

    @Override
    public String getFluentSetterMethodName(String memberName, ShapeInfo parentShape, ShapeInfo shape) {
        String setterMethodName = unCapitalize(memberName);
        setterMethodName = rewriteInvalidMemberName(setterMethodName, parentShape);

        if (shape.isOrContainsEnum() && (shape.isList() || shape.isMap())) {
            setterMethodName += "WithStrings";
        }

        return setterMethodName;
    }

    @Override
    public String getFluentEnumSetterMethodName(String memberName, ShapeInfo parentShape, ShapeInfo shape) {
        if (!shape.isOrContainsEnum()) {
            return null;
        }
        String setterMethodName = unCapitalize(memberName);
        setterMethodName = rewriteInvalidMemberName(setterMethodName, parentShape);
        return setterMethodName;
    }

    @Override
    public String getSdkFieldFieldName(MemberModel memberModel) {
        return screamCase(memberModel.getName()) + "_FIELD";
    }

    @Override
    public String getUnionEnumTypeName(MemberModel memberModel) {
        return screamCase(memberModel.getName());
    }

    @Override
    public String getExistenceCheckMethodName(String memberName, ShapeInfo parentShape) {
        String existenceCheckMethodName = unCapitalize(memberName);
        existenceCheckMethodName = rewriteInvalidMemberName(existenceCheckMethodName, parentShape);
        return String.format("has%s", Utils.capitalize(existenceCheckMethodName));
    }

    @Override
    public String getSigningName() {
        return service.getTrait(SigV4Trait.class)
                      .map(SigV4Trait::getName)
                      .orElseGet(() -> service.getTrait(ServiceTrait.class)
                                              .map(ServiceTrait::getArnNamespace)
                                              .orElse(""));
    }

    @Override
    public String getSigningNameForEnvironmentVariables() {
        return screamCase(getSigningName());
    }

    @Override
    public String getSigningNameForSystemProperties() {
        return pascalCase(getSigningName());
    }

    @Override
    public void validateCustomerVisibleNaming(IntermediateModel trimmedModel) {
        // TODO(smithy-migration): port DefaultNamingStrategy.validateCustomerVisibleNaming so Smithy-derived
        // IntermediateModels get the same customer-visible name checks. Tracked with the next AddSmithyShapes PR.
    }

    private String serviceId() {
        return service.getTrait(ServiceTrait.class)
                      .map(ServiceTrait::getSdkId)
                      .orElseThrow(() -> new IllegalStateException("Service is missing @aws.api#service trait: "
                                                                   + service.getId()));
    }

    private static String removeRedundantPrefixesAndSuffixes(String baseName) {
        baseName = Utils.removeLeading(baseName, "amazon");
        baseName = Utils.removeLeading(baseName, "aws");
        baseName = Utils.removeTrailing(baseName, "service");
        return baseName;
    }

    private String getCustomizedPackageName(String serviceName, String defaultPattern) {
        return String.format(defaultPattern, StringUtils.lowerCase(serviceName));
    }

    private static boolean isJavaKeyword(String word) {
        return RESERVED_KEYWORDS.contains(word)
               || RESERVED_KEYWORDS.contains(StringUtils.lowerCase(word));
    }

    private String screamCase(String word) {
        return Stream.of(splitOnWordBoundaries(word)).map(s -> s.toUpperCase(Locale.US)).collect(joining("_"));
    }

    private String rewriteInvalidMemberName(String memberName, ShapeInfo parentShape) {
        if (isJavaKeyword(memberName) || isDisallowedNameForShape(memberName, parentShape)) {
            return unCapitalize(memberName + CONFLICTING_NAME_SUFFIX);
        }
        return memberName;
    }

    private boolean isDisallowedNameForShape(String name, ShapeInfo parentShape) {
        if (parentShape.isUnion() && "type".equals(name)) {
            return true;
        }
        if (parentShape.isException()) {
            return RESERVED_EXCEPTION_METHOD_NAMES.contains(name);
        }
        return RESERVED_STRUCTURE_METHOD_NAMES.contains(name);
    }
}
