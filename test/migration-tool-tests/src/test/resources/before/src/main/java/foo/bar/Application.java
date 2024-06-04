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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Application {

    private Application() {

    }

    public static void main(String... args) {
        AmazonSQS sqs = SdkClientsDependencyFactory.sqsClientWithAllSettings();
        ListQueuesRequest request = new ListQueuesRequest()
            .withMaxResults(5)
            .withQueueNamePrefix("MyQueue-")
            .withNextToken("token");
        ListQueuesResult listQueuesResult = sqs.listQueues(request);
        String token = listQueuesResult.getNextToken();
        System.out.println(listQueuesResult);
    }

    private static Path downloadFile(AmazonS3 s3, String bucket, String key, Path dst) throws IOException {
        GetObjectRequest getObject = new GetObjectRequest(bucket, key);

        S3Object s3Object = s3.getObject(getObject);

        InputStream content = s3Object.getObjectContent();

        Files.copy(content, dst, StandardCopyOption.REPLACE_EXISTING);

        s3Object.getObjectContent().close();

        return dst;
    }
}
