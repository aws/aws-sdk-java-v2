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

package software.amazon.awssdk.codegen.model.intermediate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.codegen.docs.ClientType;
import software.amazon.awssdk.codegen.docs.DocConfiguration;
import software.amazon.awssdk.codegen.docs.OperationDocs;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.EndpointTrait;

public class OperationModel extends DocumentationModel {

    private String operationName;

    private boolean deprecated;

    private VariableModel input;

    private ReturnTypeModel returnType;

    private List<ExceptionModel> exceptions = new ArrayList<>();

    private List<SimpleMethodFormModel> simpleMethods;

    private boolean hasBlobMemberAsPayload;

    private boolean isAuthenticated = true;

    private AuthType authType;

    private boolean isPaginated;

    private boolean endpointOperation;

    private boolean endpointCacheRequired;

    private EndpointDiscovery endpointDiscovery;

    @JsonIgnore
    private ShapeModel inputShape;

    @JsonIgnore
    private ShapeModel outputShape;

    private EndpointTrait endpointTrait;

    private boolean httpChecksumRequired;

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getMethodName() {
        return Utils.unCapitalize(operationName);
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDocs(IntermediateModel model,
                          ClientType clientType) {
        return OperationDocs.getDocs(model, this, clientType);
    }

    public String getDocs(IntermediateModel model,
                          ClientType clientType,
                          SimpleMethodOverload methodOverload) {
        return OperationDocs.getDocs(model, this, clientType, methodOverload);
    }

    public String getDocs(IntermediateModel model,
                          ClientType clientType,
                          SimpleMethodOverload methodOverload,
                          DocConfiguration config) {
        return OperationDocs.getDocs(model, this, clientType, methodOverload, config);
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setIsAuthenticated(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public ShapeModel getInputShape() {
        return inputShape;
    }

    public void setInputShape(ShapeModel inputShape) {
        this.inputShape = inputShape;
    }

    public ShapeModel getOutputShape() {
        return outputShape;
    }

    public void setOutputShape(ShapeModel outputShape) {
        this.outputShape = outputShape;
    }

    public VariableModel getInput() {
        return input;
    }

    public void setInput(VariableModel input) {
        this.input = input;
    }

    public ReturnTypeModel getReturnType() {
        return returnType;
    }

    public void setReturnType(ReturnTypeModel returnType) {
        this.returnType = returnType;
    }

    public String getSyncReturnType() {
        return returnType.getReturnType();
    }

    public List<ExceptionModel> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<ExceptionModel> exceptions) {
        this.exceptions = exceptions;
    }

    public void addException(ExceptionModel exception) {
        exceptions.add(exception);
    }

    @JsonIgnore
    public List<SimpleMethodFormModel> getSimpleMethodForms() {
        return simpleMethods;
    }

    public void addSimpleMethodForm(List<ArgumentModel> arguments) {
        if (this.simpleMethods == null) {
            this.simpleMethods = new ArrayList<>();
        }

        SimpleMethodFormModel form = new SimpleMethodFormModel();
        form.setArguments(arguments);

        this.simpleMethods.add(form);
    }

    public boolean getHasBlobMemberAsPayload() {
        return this.hasBlobMemberAsPayload;
    }

    public void setHasBlobMemberAsPayload(boolean hasBlobMemberAsPayload) {
        this.hasBlobMemberAsPayload = hasBlobMemberAsPayload;
    }

    public boolean hasStreamingInput() {
        return inputShape != null && inputShape.isHasStreamingMember();
    }

    public boolean hasStreamingOutput() {
        return outputShape != null && outputShape.isHasStreamingMember();
    }

    @JsonIgnore
    public boolean isStreaming() {
        return hasStreamingInput() || hasStreamingOutput();
    }

    public boolean isEndpointOperation() {
        return endpointOperation;
    }

    public void setEndpointOperation(boolean endpointOperation) {
        this.endpointOperation = endpointOperation;
    }

    public boolean isEndpointCacheRequired() {
        return endpointCacheRequired;
    }

    public void setEndpointCacheRequired(boolean endpointCacheRequired) {
        this.endpointCacheRequired = endpointCacheRequired;
    }

    public boolean isPaginated() {
        return isPaginated;
    }

    public void setPaginated(boolean paginated) {
        isPaginated = paginated;
    }

    public EndpointDiscovery getEndpointDiscovery() {
        return endpointDiscovery;
    }

    public void setEndpointDiscovery(EndpointDiscovery endpointDiscovery) {
        this.endpointDiscovery = endpointDiscovery;
    }

    /**
     * Returns the endpoint trait that will be used to resolve the endpoint of an API.
     */
    public EndpointTrait getEndpointTrait() {
        return endpointTrait;
    }

    /**
     * Sets the endpoint trait that will be used to resolve the endpoint of an API.
     */
    public void setEndpointTrait(EndpointTrait endpointTrait) {
        this.endpointTrait = endpointTrait;
    }

    /**
     * @return True if the operation has an event stream member in the output shape. False otherwise.
     */
    public boolean hasEventStreamOutput() {
        return containsEventStream(outputShape);
    }

    /**
     * @return True if the operation has an event stream member in the input shape. False otherwise.
     */
    public boolean hasEventStreamInput() {
        return containsEventStream(inputShape);
    }

    public boolean hasRequiresLengthInInput() {
        return inputShape != null && inputShape.isHasRequiresLengthMember();
    }

    private boolean containsEventStream(ShapeModel shapeModel) {
        return shapeModel != null
               && shapeModel.getMembers() != null
               && shapeModel.getMembers().stream()
                            .filter(m -> m.getShape() != null)
                            .anyMatch(m -> m.getShape().isEventStream());
    }

    public boolean isHttpChecksumRequired() {
        return httpChecksumRequired;
    }

    public void setHttpChecksumRequired(boolean httpChecksumRequired) {
        this.httpChecksumRequired = httpChecksumRequired;
    }
}
