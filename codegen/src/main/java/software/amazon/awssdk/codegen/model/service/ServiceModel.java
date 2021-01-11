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

package software.amazon.awssdk.codegen.model.service;

import java.util.Collections;
import java.util.Map;

public class ServiceModel {

    private ServiceMetadata metadata;
    private Map<String, Operation> operations;
    private Map<String, Shape> shapes;
    private Map<String, Authorizer> authorizers;

    private String documentation;

    public ServiceModel() {
    }

    public ServiceModel(ServiceMetadata metadata,
                        Map<String, Operation> operations,
                        Map<String, Shape> shapes,
                        Map<String, Authorizer> authorizers) {
        this.metadata = metadata;
        this.operations = operations;
        this.shapes = shapes;
        this.authorizers = authorizers;
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ServiceMetadata metadata) {
        this.metadata = metadata;
    }

    public Map<String, Operation> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Operation> operations) {
        this.operations = operations != null ? operations : Collections.emptyMap();
    }

    /**
     * Convenience getter to retrieve an {@link Operation} by name.
     *
     * @param operationName Name of operation to retrieve.
     * @return Operation or null if not found.
     */
    public Operation getOperation(String operationName) {
        return operations.get(operationName);
    }

    public Map<String, Shape> getShapes() {
        return shapes;
    }

    public void setShapes(Map<String, Shape> shapes) {
        this.shapes = shapes != null ? shapes : Collections.emptyMap();
    }

    /**
     * Convenience getter to retrieve a {@link Shape} by name.
     *
     * @param shapeName Name of shape to retrieve.
     * @return Shape or null if not found.
     */
    public Shape getShape(String shapeName) {
        return shapes.get(shapeName);
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public Map<String, Authorizer> getAuthorizers() {
        return authorizers != null ? authorizers : Collections.emptyMap();
    }

    public void setAuthorizers(Map<String, Authorizer> authorizers) {
        this.authorizers = authorizers;
    }
}
