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

import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.smithy.model.Model;

public class DefaultSmithyNamingStrategy implements NamingStrategy {
    private final Model smithyModel;
    private final CustomizationConfig customizationConfig;

    public DefaultSmithyNamingStrategy(Model smithyModel, CustomizationConfig customizationConfig) {
        this.smithyModel = smithyModel;
        this.customizationConfig = customizationConfig;
    }

    @Override
    public String getServiceName() {
        return "";
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
        return "";
    }

    @Override
    public String getRequestClassName(String operationName) {
        return "";
    }

    @Override
    public String getResponseClassName(String operationName) {
        return "";
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
        return "";
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
}
