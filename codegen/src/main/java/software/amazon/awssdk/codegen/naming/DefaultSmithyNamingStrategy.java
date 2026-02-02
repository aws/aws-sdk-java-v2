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

import static software.amazon.awssdk.codegen.internal.Constant.REQUEST_CLASS_SUFFIX;
import static software.amazon.awssdk.utils.internal.CodegenNamingUtils.pascalCase;

import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

public class DefaultSmithyNamingStrategy implements NamingStrategy {
    private static final String COLLISION_DISAMBIGUATION_PREFIX = "Default";

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
        return "";
    }

    @Override
    public String getServiceNameForSystemProperties() {
        return "";
    }

    @Override
    public String getServiceNameForProfileFile() {
        return "";
    }

    @Override
    public String getClientPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getModelPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getTransformPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getRequestTransformPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getPaginatorsPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getWaitersPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getEndpointRulesPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getAuthSchemePackageName(String serviceName) {
        return "";
    }

    @Override
    public String getJmesPathPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getBatchManagerPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getSmokeTestPackageName(String serviceName) {
        return "";
    }

    @Override
    public String getExceptionName(String errorShapeName) {
        String baseName = pascalCase(errorShapeName);
        if (!baseName.endsWith("Exception")) {
            baseName += "Exception";
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
        String baseName = pascalCase(operationName) + "Response";
        if (!operationName.equals(getServiceName())) {
            return baseName;
        }

        return COLLISION_DISAMBIGUATION_PREFIX + baseName;
    }

    @Override
    public String getVariableName(String name) {
        return "";
    }

    @Override
    public String getEnumValueName(String enumValue) {
        return "";
    }

    @Override
    public String getShapeClassName(String shapeName) {
        return pascalCase(shapeName);
    }

    @Override
    public String getFluentGetterMethodName(String memberName, Shape parentShape, Shape shape) {
        return "";
    }

    @Override
    public String getFluentEnumGetterMethodName(String memberName, Shape parentShape, Shape shape) {
        return "";
    }

    @Override
    public String getBeanStyleGetterMethodName(String memberName, Shape parentShape, Shape c2jShape) {
        return "";
    }

    @Override
    public String getBeanStyleSetterMethodName(String memberName, Shape parentShape, Shape c2jShape) {
        return "";
    }

    @Override
    public String getFluentSetterMethodName(String memberName, Shape parentShape, Shape shape) {
        return "";
    }

    @Override
    public String getFluentEnumSetterMethodName(String memberName, Shape parentShape, Shape shape) {
        return "";
    }

    @Override
    public String getSdkFieldFieldName(MemberModel memberModel) {
        return "";
    }

    @Override
    public String getUnionEnumTypeName(MemberModel memberModel) {
        return "";
    }

    @Override
    public String getExistenceCheckMethodName(String memberName, Shape parentShape) {
        return "";
    }

    @Override
    public String getSigningName() {
        return "";
    }

    @Override
    public String getSigningNameForEnvironmentVariables() {
        return "";
    }

    @Override
    public String getSigningNameForSystemProperties() {
        return "";
    }

    @Override
    public void validateCustomerVisibleNaming(IntermediateModel trimmedModel) {

    }

    private String serviceId() {
        return service.getTrait(ServiceTrait.class)
            .map(t -> t.getSdkId())
            .orElseThrow(() -> new IllegalStateException("ServiceId is missing in the c2j model."));
    }


    // TODO: These are duplicated from DefaultNamingStrategy, Either extract common logic to a name utils OR
    // refactor both classes to extract a common impl and inherit from it.
    private static String removeRedundantPrefixesAndSuffixes(String baseName) {
        baseName = Utils.removeLeading(baseName, "amazon");
        baseName = Utils.removeLeading(baseName, "aws");
        baseName = Utils.removeTrailing(baseName, "service");
        return baseName;
    }
}
