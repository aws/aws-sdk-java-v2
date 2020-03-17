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
package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.charset.StandardCharsets;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;

public class KeysWithLeadingSlashIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(KeysWithLeadingSlashIntegrationTest.class);
    private static final String KEY = "/stupidkeywithillegalleadingslashthatsucks";
    private static final byte[] CONTENT = "Hello".getBytes(StandardCharsets.UTF_8);

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET);
    }

    @AfterClass
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void putObject_KeyWithLeadingSlash_Succeeds() {
        s3.putObject(r -> r.bucket(BUCKET).key(KEY), RequestBody.fromBytes(CONTENT));
        String retrievedKey = s3.listObjects(r -> r.bucket(BUCKET)).contents().get(0).key();

        assertThat(retrievedKey).isEqualTo(KEY);
    }
}
