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

package software.amazon.awssdk.codegen.customization.processors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.codegen.model.service.EndpointTrait;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

public class S3ControlRemoveAccountIdHostPrefixProcessorTest {
    private static final String WITH_ACCOUNT_ID_PREFIX = "WithBucketInUri";
    private static final String WITHOUT_ACCOUNT_ID_PREFIX = "WithoutBucketInUri";
    private static final String ACCOUNT_ID_PREFIX = "{AccountId}.";

    private final S3ControlRemoveAccountIdHostPrefixProcessor processor = new S3ControlRemoveAccountIdHostPrefixProcessor();

    @Test
    public void preprocess_serviceNotS3Control_doesNotModify() {
        ServiceModel nonS3 = createServiceModel("NonS3Control");
        processor.preprocess(nonS3);

        Map<String, Operation> operations = nonS3.getOperations();

        assertThat(operations.get(WITH_ACCOUNT_ID_PREFIX).getEndpoint().getHostPrefix()).isEqualTo(ACCOUNT_ID_PREFIX);
        assertThat(operations.get(WITHOUT_ACCOUNT_ID_PREFIX).getEndpoint()).isNull();
    }

    @Test
    public void preprocess_serviceIsS3Control_operationAccountIdPrefix_modifiesPrefix() {
        ServiceModel s3Control = createServiceModel("S3 Control");
        processor.preprocess(s3Control);

        Map<String, Operation> operations = s3Control.getOperations();

        assertThat(operations.get(WITH_ACCOUNT_ID_PREFIX).getEndpoint().getHostPrefix()).isNull();
    }

    @Test
    public void preprocess_serviceIsS3Control_operationDoesNotAccountIdPrefix_noModification() {
        ServiceModel s3Control = createServiceModel("S3 Control");
        processor.preprocess(s3Control);

        Map<String, Operation> operations = s3Control.getOperations();

        assertThat(operations.get(WITHOUT_ACCOUNT_ID_PREFIX).getEndpoint()).isNull();;
    }

    private static ServiceModel createServiceModel(String serviceId) {
        ServiceModel model = new ServiceModel();

        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setServiceId(serviceId);
        model.setMetadata(serviceMetadata);

        Operation operationWithoutAccountIdPrefix = new Operation();

        Operation operationWithAccountIdPrefix = new Operation();
        EndpointTrait endpointTrait = new EndpointTrait();
        endpointTrait.setHostPrefix(ACCOUNT_ID_PREFIX);
        operationWithAccountIdPrefix.setEndpoint(endpointTrait);

        Map<String, Operation> operations = new HashMap<>();
        operations.put(WITH_ACCOUNT_ID_PREFIX, operationWithAccountIdPrefix);
        operations.put(WITHOUT_ACCOUNT_ID_PREFIX, operationWithoutAccountIdPrefix);

        model.setOperations(operations);

        return model;
    }
}
