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

package software.amazon.awssdk.services.s3.crthttpclient;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.utils.ChecksumUtils;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.Md5Utils;

public class S3WithCrtHttpClientsIntegrationTests extends S3IntegrationTestBase {
    private static final String TEST_BUCKET = temporaryBucketName(S3WithCrtHttpClientsIntegrationTests.class);
    private static final String TEST_KEY = "2mib_file.dat";
    private static final int OBJ_SIZE = 2 * 1024 * 1024;

    private static S3Client s3WithCrtHttpClient;
    private static RandomTempFile testFile;

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(TEST_BUCKET);
        testFile = new RandomTempFile(TEST_KEY, OBJ_SIZE);
        s3WithCrtHttpClient = s3ClientBuilderWithCrtHttpClient().build();
        s3WithCrtHttpClient.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), RequestBody.fromFile(testFile.toPath()));
    }

    @AfterAll
    public static void teardown() throws IOException {
        S3IntegrationTestBase.deleteBucketAndAllContents(TEST_BUCKET);
        Files.delete(testFile.toPath());
        s3WithCrtHttpClient.close();

    }

    @Test
    void getObject_toResponseStream_objectSentCorrectly() throws Exception {
        ResponseInputStream<GetObjectResponse> objContent =
            s3WithCrtHttpClient.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                          ResponseTransformer.toInputStream());

        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));

        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    void getObject_toFile_objectSentCorrectly() throws Exception {
        Path destination = RandomTempFile.randomUncreatedFile().toPath();
        GetObjectResponse response = s3WithCrtHttpClient.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                 ResponseTransformer.toFile(destination));

        assertThat(Md5Utils.md5AsBase64(destination.toFile())).isEqualTo(Md5Utils.md5AsBase64(testFile));
    }
}
