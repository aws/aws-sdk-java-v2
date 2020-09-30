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

package software.amazon.awssdk.codegen.poet;

import com.squareup.javapoet.ClassName;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;

/**
 * Extension and convenience methods to Poet that use the intermediate model.
 */
public class PoetExtensions {

    private final IntermediateModel model;

    public PoetExtensions(IntermediateModel model) {
        this.model = model;
    }

    /**
     * @param className Simple name of class in model package.
     * @return A Poet {@link ClassName} for the given class in the model package.
     */
    public ClassName getModelClass(String className) {
        return ClassName.get(model.getMetadata().getFullModelPackageName(), className);
    }

    /**
     * @param className Simple name of class in transform package.
     * @return A Poet {@link ClassName} for the given class in the transform package.
     */
    public ClassName getTransformClass(String className) {
        return ClassName.get(model.getMetadata().getFullTransformPackageName(), className);
    }

    /**
     * @param className Simple name of class in transform package.
     * @return A Poet {@link ClassName} for the given class in the transform package.
     */
    public ClassName getRequestTransformClass(String className) {
        return ClassName.get(model.getMetadata().getFullRequestTransformPackageName(), className);
    }

    /**
     * @param className Simple name of class in base service package (i.e. software.amazon.awssdk.services.dynamodb).
     * @return A Poet {@link ClassName} for the given class in the base service package.
     */
    public ClassName getClientClass(String className) {
        return ClassName.get(model.getMetadata().getFullClientPackageName(), className);
    }

    /**
     * @param operationName Name of the operation
     * @return A Poet {@link ClassName} for the response type of a paginated operation in the base service package.
     *
     * Example: If operationName is "ListTables", then the response type of the paginated operation
     * will be "ListTablesIterable" class.
     */
    public ClassName getResponseClassForPaginatedSyncOperation(String operationName) {
        return ClassName.get(model.getMetadata().getFullPaginatorsPackageName(), operationName + "Iterable");
    }

    public ClassName getSyncWaiterInterface() {
        return ClassName.get(model.getMetadata().getFullWaitersPackageName(), model.getMetadata().getServiceName() + "Waiter");
    }

    public ClassName getSyncWaiterClass() {
        return ClassName.get(model.getMetadata().getFullWaitersPackageName(), "Default" + model.getMetadata().getServiceName() +
                                                                              "Waiter");
    }

    public ClassName getAsyncWaiterInterface() {
        return ClassName.get(model.getMetadata().getFullWaitersPackageName(), model.getMetadata().getServiceName() +
                                                                              "AsyncWaiter");
    }

    public ClassName getAsyncWaiterClass() {
        return ClassName.get(model.getMetadata().getFullWaitersPackageName(), "Default" + model.getMetadata().getServiceName() +
                                                                              "AsyncWaiter");
    }

    /**
     * @param operationName Name of the operation
     * @return A Poet {@link ClassName} for the response type of a async paginated operation in the base service package.
     *
     * Example: If operationName is "ListTables", then the async response type of the paginated operation
     * will be "ListTablesPublisher" class.
     */
    public ClassName getResponseClassForPaginatedAsyncOperation(String operationName) {
        return ClassName.get(model.getMetadata().getFullPaginatorsPackageName(), operationName + "Publisher");
    }

    /**
     * @return ResponseMetadata className. eg: "S3ResponseMetadata"
     */
    public ClassName getResponseMetadataClass() {
        return ClassName.get(model.getMetadata().getFullModelPackageName(),
                             model.getSdkResponseBaseClassName() + "Metadata");
    }

    /**
     * @return The correctly cased name of the API.
     */
    public String getApiName(OperationModel operation) {
        return Utils.capitalize(operation.getOperationName());
    }

    /**
     * @return The {@link ClassName} for the response pojo.
     */
    public ClassName responsePojoType(OperationModel operation) {
        return getModelClass(operation.getOutputShape().getShapeName());
    }

    // TODO Should we move the event stream specific methods to a new class
    /**
     * @return {@link ClassName} for generated event stream response handler interface.
     */
    public ClassName eventStreamResponseHandlerType(OperationModel operation) {
        return getModelClass(getApiName(operation) + "ResponseHandler");
    }

    /**
     * @return {@link ClassName} for the builder interface for the response handler interface
     */
    public ClassName eventStreamResponseHandlerBuilderType(OperationModel operation) {
        return eventStreamResponseHandlerType(operation).nestedClass("Builder");
    }

    /**
     * @return {@link ClassName} for the event stream visitor interface.
     */
    public ClassName eventStreamResponseHandlerVisitorType(OperationModel operation) {
        return eventStreamResponseHandlerType(operation).nestedClass("Visitor");
    }

    /**
     * @return {@link ClassName} for the builder interface for the event stream visitor interface.
     */
    public ClassName eventStreamResponseHandlerVisitorBuilderType(OperationModel operation) {
        return eventStreamResponseHandlerVisitorType(operation).nestedClass("Builder");
    }

    /**
     * @param shapeModel shape model for the class in model package
     * @return {@link ClassName} for the shape represented by the given {@link ShapeModel}.
     */
    public ClassName getModelClassFromShape(ShapeModel shapeModel) {
        return getModelClass(shapeModel.getShapeName());
    }

    public boolean isResponse(ShapeModel shapeModel) {
        return shapeModel.getShapeType() == ShapeType.Response;
    }

    public boolean isRequest(ShapeModel shapeModel) {
        return shapeModel.getShapeType() == ShapeType.Request;
    }
}
