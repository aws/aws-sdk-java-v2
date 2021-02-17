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

package software.amazon.awssdk.nativeimagetest;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.testutils.RandomInputStream;
import software.amazon.awssdk.testutils.service.S3BucketUtils;
import software.amazon.awssdk.utils.IoUtils;

public class S3TestRunner implements TestRunner {
    private static final String BUCKET_NAME = S3BucketUtils.temporaryBucketName("native-image");
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbEnhancedClientTestRunner.class);
    private static final String KEY = "key";
    private final S3Client s3ApacheHttpClient;
    private final S3Client s3UrlConnectionHttpClient;
    private final S3AsyncClient s3NettyClient;

    public S3TestRunner() {
        s3ApacheHttpClient = DependencyFactory.s3ApacheHttpClient();
        s3UrlConnectionHttpClient = DependencyFactory.s3UrlConnectionHttpClient();
        s3NettyClient = DependencyFactory.s3NettyClient();
    }

    @Override
    public void runTests() {
        logger.info("starting to run S3 tests");
        CreateBucketResponse bucketResponse = null;
        InputStream inputStream = null;
        try {
            bucketResponse = s3UrlConnectionHttpClient.createBucket(b -> b.bucket(BUCKET_NAME));

            s3UrlConnectionHttpClient.waiter().waitUntilBucketExists(b -> b.bucket(BUCKET_NAME));

            inputStream = new RandomInputStream(10_000);

            RequestBody requestBody = RequestBody.fromInputStream(inputStream, 10_000);

            s3ApacheHttpClient.putObject(b -> b.bucket(BUCKET_NAME).key(KEY),
                                         requestBody);

            s3NettyClient.getObject(b -> b.bucket(BUCKET_NAME).key(KEY),
                               AsyncResponseTransformer.toBytes()).join();

        } finally {
            if (bucketResponse != null) {
                s3NettyClient.deleteObject(b -> b.bucket(BUCKET_NAME).key(KEY)).join();

                s3NettyClient.deleteBucket(b -> b.bucket(BUCKET_NAME)).join();
            }
            IoUtils.closeQuietly(inputStream, null);
        }
    }
}
