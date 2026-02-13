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
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.utils.ProtocolUtils;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

public class DefaultSmithyNamingStrategy implements NamingStrategy {
    private static final Logger log = Logger.loggerFor(DefaultSmithyNamingStrategy.class);
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

    private final Model smithyModel;
    private final ServiceShape service;
    private final CustomizationConfig customizationConfig;

    public DefaultSmithyNamingStrategy(Model smithyModel, ServiceShape service, CustomizationConfig customizationConfig) {
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
    public String getEnumValueName(String enumValue) {
        String result = enumValue;

        // Special cases
        result = result.replace("textORcsv", "TEXT_OR_CSV");

        // leading digits, add a prefix
        if (result.matches("^\\d.*")) {
            result = "VALUE_" + result;
        }

        // Split into words
        result = String.join("_", splitOnWordBoundaries(result));

        // Enums should be upper-case
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
    public String getFluentGetterMethodName(String memberName, Shape parentShape, Shape shape) {
        String getterMethodName = unCapitalize(memberName);
        getterMethodName = rewriteInvalidMemberName(getterMethodName, parentShape);

        if (shape != null && Utils.isOrContainsEnumShape(shape, Collections.emptyMap())) {
            getterMethodName += "AsString";
            if (Utils.isListShape(shape) || Utils.isMapShape(shape)) {
                getterMethodName += "s";
            }
        }

        return getterMethodName;
    }

    /**
     * Smithy-aware fluent getter that uses boolean flags instead of C2J Shape objects.
     */
    public String getFluentGetterMethodName(String memberName, boolean isException, boolean isUnion,
                                            boolean isEnum, boolean isList, boolean isMap) {
        String getterMethodName = unCapitalize(memberName);
        getterMethodName = rewriteInvalidMemberName(getterMethodName, isException, isUnion);

        if (isEnum) {
            getterMethodName += "AsString";
            if (isList || isMap) {
                getterMethodName += "s";
            }
        }

        return getterMethodName;
    }

    @Override
    public String getFluentEnumGetterMethodName(String memberName, Shape parentShape, Shape shape) {
        if (shape != null && !Utils.isOrContainsEnumShape(shape, Collections.emptyMap())) {
            return null;
        }
        String getterMethodName = unCapitalize(memberName);
        getterMethodName = rewriteInvalidMemberName(getterMethodName, parentShape);
        return getterMethodName;
    }

    /**
     * Smithy-aware fluent enum getter.
     */
    public String getFluentEnumGetterMethodName(String memberName, boolean isException, boolean isUnion, boolean isEnum) {
        if (!isEnum) {
            return null;
        }
        String getterMethodName = unCapitalize(memberName);
        getterMethodName = rewriteInvalidMemberName(getterMethodName, isException, isUnion);
        return getterMethodName;
    }

    @Override
    public String getBeanStyleGetterMethodName(String memberName, Shape parentShape, Shape c2jShape) {
        String fluentGetterMethodName;
        if (c2jShape != null && Utils.isOrContainsEnumShape(c2jShape, Collections.emptyMap())) {
            fluentGetterMethodName = getFluentEnumGetterMethodName(memberName, parentShape, c2jShape);
        } else {
            fluentGetterMethodName = getFluentGetterMethodName(memberName, parentShape, c2jShape);
        }
        return String.format("get%s", Utils.capitalize(fluentGetterMethodName));
    }

    /**
     * Smithy-aware bean-style getter.
     */
    public String getBeanStyleGetterMethodName(String memberName, boolean isException, boolean isUnion, boolean isEnum) {
        String fluentGetterMethodName;
        if (isEnum) {
            fluentGetterMethodName = getFluentEnumGetterMethodName(memberName, isException, isUnion, isEnum);
        } else {
            fluentGetterMethodName = getFluentGetterMethodName(memberName, isException, isUnion, false, false, false);
        }
        return String.format("get%s", Utils.capitalize(fluentGetterMethodName));
    }

    @Override
    public String getBeanStyleSetterMethodName(String memberName, Shape parentShape, Shape c2jShape) {
        String beanStyleGetter = getBeanStyleGetterMethodName(memberName, parentShape, c2jShape);
        return String.format("set%s", beanStyleGetter.substring("get".length()));
    }

    /**
     * Smithy-aware bean-style setter.
     */
    public String getBeanStyleSetterMethodName(String memberName, boolean isException, boolean isUnion, boolean isEnum) {
        String beanStyleGetter = getBeanStyleGetterMethodName(memberName, isException, isUnion, isEnum);
        return String.format("set%s", beanStyleGetter.substring("get".length()));
    }

    @Override
    public String getFluentSetterMethodName(String memberName, Shape parentShape, Shape shape) {
        String setterMethodName = unCapitalize(memberName);
        setterMethodName = rewriteInvalidMemberName(setterMethodName, parentShape);

        if (shape != null && Utils.isOrContainsEnumShape(shape, Collections.emptyMap())
            && (Utils.isListShape(shape) || Utils.isMapShape(shape))) {
            setterMethodName += "WithStrings";
        }

        return setterMethodName;
    }

    /**
     * Smithy-aware fluent setter.
     */
    public String getFluentSetterMethodName(String memberName, boolean isException, boolean isUnion,
                                            boolean isEnum, boolean isList, boolean isMap) {
        String setterMethodName = unCapitalize(memberName);
        setterMethodName = rewriteInvalidMemberName(setterMethodName, isException, isUnion);

        if (isEnum && (isList || isMap)) {
            setterMethodName += "WithStrings";
        }

        return setterMethodName;
    }

    @Override
    public String getFluentEnumSetterMethodName(String memberName, Shape parentShape, Shape shape) {
        if (shape != null && !Utils.isOrContainsEnumShape(shape, Collections.emptyMap())) {
            return null;
        }
        String setterMethodName = unCapitalize(memberName);
        setterMethodName = rewriteInvalidMemberName(setterMethodName, parentShape);
        return setterMethodName;
    }

    /**
     * Smithy-aware fluent enum setter.
     */
    public String getFluentEnumSetterMethodName(String memberName, boolean isException, boolean isUnion, boolean isEnum) {
        if (!isEnum) {
            return null;
        }
        String setterMethodName = unCapitalize(memberName);
        setterMethodName = rewriteInvalidMemberName(setterMethodName, isException, isUnion);
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
    public String getExistenceCheckMethodName(String memberName, Shape parentShape) {
        String existenceCheckMethodName = unCapitalize(memberName);
        existenceCheckMethodName = rewriteInvalidMemberName(existenceCheckMethodName, parentShape);
        return String.format("has%s", Utils.capitalize(existenceCheckMethodName));
    }

    /**
     * Smithy-aware existence check method name.
     */
    public String getExistenceCheckMethodName(String memberName, boolean isException, boolean isUnion) {
        String existenceCheckMethodName = unCapitalize(memberName);
        existenceCheckMethodName = rewriteInvalidMemberName(existenceCheckMethodName, isException, isUnion);
        return String.format("has%s", Utils.capitalize(existenceCheckMethodName));
    }

    @Override
    public String getSigningName() {
        return service.getTrait(software.amazon.smithy.aws.traits.auth.SigV4Trait.class)
                      .map(sigv4 -> sigv4.getName())
                      .orElseGet(() -> service.getTrait(ServiceTrait.class)
                                              .map(st -> st.getArnNamespace())
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
        // TODO: Implement validation for Smithy models
    }

    private String serviceId() {
        return service.getTrait(ServiceTrait.class)
                      .map(t -> t.getSdkId())
                      .orElseThrow(() -> new IllegalStateException("ServiceId is missing in the Smithy model."));
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

    /**
     * Rewrite invalid member names using C2J Shape (for interface compatibility).
     */
    private String rewriteInvalidMemberName(String memberName, Shape parentShape) {
        if (parentShape == null) {
            // Without parent shape info, just check keywords and structure method names
            if (isJavaKeyword(memberName) || RESERVED_STRUCTURE_METHOD_NAMES.contains(memberName)) {
                return unCapitalize(memberName + CONFLICTING_NAME_SUFFIX);
            }
            return memberName;
        }

        if (isJavaKeyword(memberName) || isDisallowedNameForShape(memberName, parentShape)) {
            return unCapitalize(memberName + CONFLICTING_NAME_SUFFIX);
        }
        return memberName;
    }

    /**
     * Rewrite invalid member names using Smithy-derived boolean flags.
     */
    private String rewriteInvalidMemberName(String memberName, boolean isException, boolean isUnion) {
        if (isUnion && "type".equals(memberName)) {
            return unCapitalize(memberName + CONFLICTING_NAME_SUFFIX);
        }

        if (isJavaKeyword(memberName)) {
            return unCapitalize(memberName + CONFLICTING_NAME_SUFFIX);
        }

        if (isException) {
            if (RESERVED_EXCEPTION_METHOD_NAMES.contains(memberName)) {
                return unCapitalize(memberName + CONFLICTING_NAME_SUFFIX);
            }
        } else {
            if (RESERVED_STRUCTURE_METHOD_NAMES.contains(memberName)) {
                return unCapitalize(memberName + CONFLICTING_NAME_SUFFIX);
            }
        }

        return memberName;
    }

    private boolean isDisallowedNameForShape(String name, Shape parentShape) {
        if (parentShape.isUnion() && "type".equals(name)) {
            return true;
        }
        if (parentShape.isException()) {
            return RESERVED_EXCEPTION_METHOD_NAMES.contains(name);
        } else {
            return RESERVED_STRUCTURE_METHOD_NAMES.contains(name);
        }
    }
}
