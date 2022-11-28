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
import software.amazon.awssdk.codegen.model.service.Http;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

public class S3RemoveBucketFromUriProcessorTest {
    private static final String WITH_BUCKET_URI_OPERATION = "WithBucketInUri";
    private static final String WITHOUT_BUCKET_URI_OPERATION = "WithoutBucketInUri";
    private static final String WITH_BUCKET_URI = "/{Bucket}/{Key+}";
    private static final String WITHOUT_BUCKET_URI = "/{Key+}";

    private final S3RemoveBucketFromUriProcessor processor = new S3RemoveBucketFromUriProcessor();

    @Test
    public void preprocess_serviceNotS3_doesNotModifyUri() {
        ServiceModel nonS3 = createServiceModel("NonS3");
        processor.preprocess(nonS3);

        Map<String, Operation> operations = nonS3.getOperations();

        assertThat(operations.get(WITH_BUCKET_URI_OPERATION).getHttp().getRequestUri()).isEqualTo(WITH_BUCKET_URI);
        assertThat(operations.get(WITHOUT_BUCKET_URI_OPERATION).getHttp().getRequestUri()).isEqualTo(WITHOUT_BUCKET_URI);
    }

    @Test
    public void preprocess_serviceIsS3_operationHasBucket_modifiesUri() {
        ServiceModel s3 = createServiceModel("S3");
        processor.preprocess(s3);

        Map<String, Operation> operations = s3.getOperations();

        assertThat(operations.get(WITH_BUCKET_URI_OPERATION).getHttp().getRequestUri()).isEqualTo("/{Key+}");
    }

    @Test
    public void preprocess_serviceIsS3_operationDoesNotHaveBucket_noModification() {
        ServiceModel s3 = createServiceModel("S3");
        processor.preprocess(s3);

        Map<String, Operation> operations = s3.getOperations();

        assertThat(operations.get(WITHOUT_BUCKET_URI_OPERATION).getHttp().getRequestUri()).isEqualTo(WITHOUT_BUCKET_URI);
    }

    private static ServiceModel createServiceModel(String serviceId) {
        ServiceModel model = new ServiceModel();

        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setServiceId(serviceId);
        model.setMetadata(serviceMetadata);

        Operation operationWithBucketInUri = new Operation()
            .withHttp(new Http().withRequestUri(WITH_BUCKET_URI));

        Operation operationWithoutBucketInUri = new Operation()
            .withHttp(new Http().withRequestUri(WITHOUT_BUCKET_URI));

        Map<String, Operation> operations = new HashMap<>();
        operations.put(WITH_BUCKET_URI_OPERATION, operationWithBucketInUri);
        operations.put(WITHOUT_BUCKET_URI_OPERATION, operationWithoutBucketInUri);

        model.setOperations(operations);

        return model;
    }
}
