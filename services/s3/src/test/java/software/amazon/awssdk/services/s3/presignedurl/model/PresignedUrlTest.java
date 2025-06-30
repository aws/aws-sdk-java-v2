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

package software.amazon.awssdk.services.s3.presignedurl.model;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;


public class PresignedUrlTest {
    @Test
    void presignedUrlGetTest(){
        String presignedUrl = "https://example-bucket.s3.amazonaws.com/test-object.txt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=EXAMPLE123%2F20250101%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20250101T000000Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=example1234567890abcdef";
        S3Client s3Client = S3Client.builder()
                                    .build();
        ResponseBytes<GetObjectResponse> response = s3Client
            .presignedUrlManager()
            .getObject(PresignedUrlGetObjectRequest.builder()
                                                   .presignedUrl(presignedUrl)
                                                   .range("bytes=0-5")
                                                   .build(), ResponseTransformer.toBytes());

        System.out.println("Content length: " + response.asByteArray().length);
        System.out.println("Content range: " + response.response().contentRange());


        // // Generate a presigned URL
        // S3Presigner presigner = S3Presigner.builder()
        //                                    .build();
        // GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        //                                                    .bucket("jency-test-bucket")
        //                                                    .key("test1.txt")
        //                                                    .build();
        // GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        //                                                                 .signatureDuration(java.time.Duration.ofDays(5))
        //                                                                 .getObjectRequest(getObjectRequest)
        //                                                                 .build();
        // PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        // String presignedUrl = presignedRequest.url().toString();
        // System.out.println("Presigned URL: " + presignedUrl);

    }

}
