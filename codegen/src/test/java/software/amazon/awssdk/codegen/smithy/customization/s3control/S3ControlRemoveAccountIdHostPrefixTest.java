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

package software.amazon.awssdk.codegen.smithy.customization.s3control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.EndpointTrait;

class S3ControlRemoveAccountIdHostPrefixTest {

    @Test
    void removesOnlyTheAccountIdHostPrefix() {
        Map<String, OperationModel> operations = new HashMap<>();
        operations.put("GetAccessPoint", operation("GetAccessPoint", "{AccountId}."));
        operations.put("GetBucket", operation("GetBucket", "{Bucket}."));
        operations.put("ListRegionalBuckets", operation("ListRegionalBuckets", null));
        IntermediateModel model = new IntermediateModel(new Metadata(), operations, new HashMap<>(),
                                                        CustomizationConfig.create());

        new S3ControlRemoveAccountIdHostPrefix().postprocess(model);

        assertThat(model.getOperation("GetAccessPoint").getEndpointTrait().getHostPrefix()).isNull();
        assertThat(model.getOperation("GetBucket").getEndpointTrait().getHostPrefix()).isEqualTo("{Bucket}.");
        assertThat(model.getOperation("ListRegionalBuckets").getEndpointTrait()).isNull();
    }

    private static OperationModel operation(String name, String hostPrefix) {
        OperationModel operation = new OperationModel();
        operation.setOperationName(name);
        if (hostPrefix != null) {
            EndpointTrait endpoint = new EndpointTrait();
            endpoint.setHostPrefix(hostPrefix);
            operation.setEndpointTrait(endpoint);
        }
        return operation;
    }
}
