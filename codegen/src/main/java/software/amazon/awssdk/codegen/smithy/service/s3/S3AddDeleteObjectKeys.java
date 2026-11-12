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

package software.amazon.awssdk.codegen.smithy.service.s3;

import com.fasterxml.jackson.jr.stree.JrsString;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.OperationContextParam;

/**
 * Adds the {@code DeleteObjectKeys} endpoint parameter, which DeleteObjects fills from the keys of the objects it
 * deletes.
 */
@SdkInternalApi
final class S3AddDeleteObjectKeys extends AbstractS3Customization {
    private static final String DELETE_OBJECT_KEYS = "DeleteObjectKeys";
    private static final String DELETE_OBJECTS = "DeleteObjects";

    @Override
    public boolean appliesConfig(String key) {
        return "endpointParameters".equals(key) || "customOperationContextParams".equals(key);
    }

    @Override
    public void postprocess(IntermediateModel model) {
        Map<String, ParameterModel> parameters = new LinkedHashMap<>(model.getEndpointParameters());
        if (parameters.putIfAbsent(DELETE_OBJECT_KEYS, deleteObjectKeys()) != null) {
            throw new IllegalStateException("Duplicate endpoint parameter " + DELETE_OBJECT_KEYS);
        }
        model.setEndpointParameters(parameters);

        OperationModel operation = model.getOperation(DELETE_OBJECTS);
        if (operation == null) {
            throw new IllegalStateException("Could not find operation " + DELETE_OBJECTS
                                            + " to add operation context params to");
        }
        if (operation.getOperationContextParams() != null) {
            throw new IllegalStateException("Operation " + DELETE_OBJECTS + " already has operation context params");
        }
        OperationContextParam keys = new OperationContextParam();
        keys.setPath(new JrsString("Delete.Objects[*].Key"));
        Map<String, OperationContextParam> params = new LinkedHashMap<>();
        params.put(DELETE_OBJECT_KEYS, keys);
        operation.setOperationContextParams(params);
    }

    private static ParameterModel deleteObjectKeys() {
        ParameterModel parameter = new ParameterModel();
        parameter.setType("StringArray");
        parameter.setRequired(false);
        parameter.setDocumentation("DeleteObjectKeys Endpoint Params from the customization config");
        return parameter;
    }
}
