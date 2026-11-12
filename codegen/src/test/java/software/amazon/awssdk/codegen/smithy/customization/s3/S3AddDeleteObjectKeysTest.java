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

package software.amazon.awssdk.codegen.smithy.customization.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.jr.stree.JrsString;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.OperationContextParam;

class S3AddDeleteObjectKeysTest {
    @Test
    void build_addsDeleteObjectKeysAndKeepsItAfterUnusedShapesAreRemoved() {
        IntermediateModel model = S3Fixtures.build();

        assertThat(model.getEndpointParameters()).containsOnlyKeys("Region", "DeleteObjectKeys");
        ParameterModel keys = model.getEndpointParameters().get("DeleteObjectKeys");
        assertThat(keys.getType()).isEqualTo("StringArray");
        assertThat(keys.isRequired()).isFalse();
    }

    @Test
    void build_fillsDeleteObjectKeysFromTheDeletedObjectKeys() {
        Map<String, OperationContextParam> params = S3Fixtures.build().getOperation("DeleteObjects")
                                                              .getOperationContextParams();

        assertThat(params).containsOnlyKeys("DeleteObjectKeys");
        assertThat(params.get("DeleteObjectKeys").getPath()).isInstanceOfSatisfying(
            JrsString.class, path -> assertThat(path.getValue()).isEqualTo("Delete.Objects[*].Key"));
    }

    @Test
    void postprocess_withoutDeleteObjects_fails() {
        IntermediateModel model = model(new HashMap<>());

        assertThatThrownBy(() -> new S3AddDeleteObjectKeys().postprocess(model))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not find operation DeleteObjects to add operation context params to");
    }

    @Test
    void postprocess_whenDeleteObjectsAlreadyHasContextParams_fails() {
        OperationModel deleteObjects = new OperationModel();
        deleteObjects.setOperationContextParams(new HashMap<>());
        Map<String, OperationModel> operations = new HashMap<>();
        operations.put("DeleteObjects", deleteObjects);
        IntermediateModel model = model(operations);

        assertThatThrownBy(() -> new S3AddDeleteObjectKeys().postprocess(model))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Operation DeleteObjects already has operation context params");
    }

    @Test
    void postprocess_whenTheRulesDeclareDeleteObjectKeys_fails() {
        IntermediateModel model = model(new HashMap<>());
        Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put("DeleteObjectKeys", new ParameterModel());
        model.setEndpointParameters(parameters);

        assertThatThrownBy(() -> new S3AddDeleteObjectKeys().postprocess(model))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicate endpoint parameter DeleteObjectKeys");
    }

    private static IntermediateModel model(Map<String, OperationModel> operations) {
        IntermediateModel model = new IntermediateModel(new Metadata(), operations, new HashMap<>(),
                                                        CustomizationConfig.create());
        model.setEndpointParameters(new HashMap<>());
        return model;
    }
}
