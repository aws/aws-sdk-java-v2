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

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Strategy to name various Java constructs based on the naming in the model and potentially customizations.
 */
public interface NamingStrategy {
    /**
     * Retrieve the service name that should be used based on the model.
     */
    String getServiceName();

    /**
     * Retrieve the client package name that should be used based on the service name.
     */
    String getClientPackageName(String serviceName);

    /**
     * Retrieve the model package name that should be used based on the service name.
     */
    String getModelPackageName(String serviceName);

    /**
     * Retrieve the transform package name that should be used based on the service name.
     */
    String getTransformPackageName(String serviceName);

    /**
     * Retrieve the request transform package name that should be used based on the service name.
     */
    String getRequestTransformPackageName(String serviceName);

    /**
     * Retrieve the paginators package name that should be used based on the service name.
     */
    String getPaginatorsPackageName(String serviceName);

    /**
     * Retrieve the waiters package name that should be used based on the service name.
     */
    String getWaitersPackageName(String serviceName);

    /**
     * Retrieve the smote test package name that should be used based on the service name.
     */
    String getSmokeTestPackageName(String serviceName);

    /**
     * @param errorShapeName Name of error shape to derive exception class name from.
     * @return Appropriate name to use for a Java exception class name
     */
    String getExceptionName(String errorShapeName);


    /**
     * @param operationName Name of operation used to derive request class name.
     * @return Appropriate name to use for the Java class representing the request shape.
     */
    String getRequestClassName(String operationName);

    /**
     * @param operationName Name of operation used to derive response class name.
     * @return Appropriate name to use for the Java class representing the response shape.
     */
    String getResponseClassName(String operationName);

    /**
     * @param name Some contextual name to derive variable name from (i.e. member name, java class name, etc).
     * @return Appropriate name to use for a Java variable or field.
     */
    String getVariableName(String name);

    /**
     * @param enumValue Enum value as defined in the service model used to derive the java name.
     * @return Appropriate name to use for a Java enum value
     */
    String getEnumValueName(String enumValue);

    /**
     * @param shapeName Name of structure used to derive Java class name.
     * @return Appropriate name to use for a Java class for an arbitrary (not a request, response, error) structure.
     */
    String getShapeClassName(String shapeName);

    /**
     * @param memberName Member name to name getter for.
     * @param shape The shape associated with the member.
     * @return Name of the getter method for a model class member.
     */
    String getFluentGetterMethodName(String memberName, Shape parentShape, Shape shape);

    /**
     * @param memberName The full member to get the name for.
     * @param shape The shape associated with the member.
     * @return Name of the getter method for an enum model class member.
     */
    String getFluentEnumGetterMethodName(String memberName, Shape parentShape, Shape shape);

    /**
     * @param memberName Member name to name getter for.
     * @return Name of the JavaBean getter method for model class member.
     */
    String getBeanStyleGetterMethodName(String memberName, Shape parentShape, Shape c2jShape);

    /**
     * @param memberName Member name to name setter for.
     * @return Name of the JavaBean setter method for model class member.
     */
    String getBeanStyleSetterMethodName(String memberName, Shape parentShape, Shape c2jShape);

    /**
     * @param memberName Member name to name fluent setter for.
     * @return Appropriate name to use for fluent setter method (i.e. withFoo) for a model class member.
     */
    String getFluentSetterMethodName(String memberName, Shape parentShape, Shape shape);

    /**
     * @param memberName The full member to get the name for.
     * @param shape The shape associated with the member.
     * @return Name of the getter method for an enum model class member.
     */
    String getFluentEnumSetterMethodName(String memberName, Shape parentShape, Shape shape);

    /**
     * Stuttering is intentional, returns the name of the {@link SdkField} field.
     *
     * @param memberModel Member to generate field name for.
     * @return Name of field for {@link SdkField} pojo.
     */
    String getSdkFieldFieldName(MemberModel memberModel);

    /**
     * Names a method that would check for existence of the member in the response.
     *
     * @param memberName The member name to get the method name for.
     * @param parentShape The shape containing the member.
     * @return Name of an existence check method.
     */
    String getExistenceCheckMethodName(String memberName, Shape parentShape);

    /**
     * Verify the customer-visible naming in the provided intermediate model will compile and is idiomatic to Java.
     */
    void validateCustomerVisibleNaming(IntermediateModel trimmedModel);
}
