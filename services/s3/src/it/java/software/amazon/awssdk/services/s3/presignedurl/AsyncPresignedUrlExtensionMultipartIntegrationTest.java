/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.awssdk.services.s3.presignedurl;

import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.services.s3.S3AsyncClient;


public class AsyncPresignedUrlExtensionMultipartIntegrationTest extends AsyncPresignedUrlExtensionTestSuite {

    @BeforeAll
    static void setUpIntegrationTest() {
        S3AsyncClient s3AsyncClient = s3AsyncClientBuilder()
            .multipartEnabled(true)
            .build();
        presignedUrlExtension = s3AsyncClient.presignedUrlExtension();
    }

    @Override
    protected S3AsyncClient createS3AsyncClient() {
        return s3AsyncClientBuilder().build();
    }
}