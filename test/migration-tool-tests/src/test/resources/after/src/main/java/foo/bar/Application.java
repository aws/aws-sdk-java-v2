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

package foo.bar;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class Application {

    private Application() {

    }

    public static void main(String... args) {
        SqsClient sqs = SdkClientsDependencyFactory.sqsClientWithAllSettings();
        ListQueuesRequest request = ListQueuesRequest.builder()
            .maxResults(5)
            .queueNamePrefix("MyQueue-")
            .nextToken("token").build();
        ListQueuesResponse listQueuesResult = sqs.listQueues(request);
        String token = listQueuesResult.nextToken();
        System.out.println(listQueuesResult);
    }

    private static Path downloadFile(S3Client s3, String bucket, String key, Path dst) throws IOException {
        GetObjectRequest getObject = GetObjectRequest.builder().bucket(bucket).key(key).build();

        ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(getObject);

        InputStream content = s3Object;

        Files.copy(content, dst, StandardCopyOption.REPLACE_EXISTING);

        s3Object.close();

        return dst;
    }
}
