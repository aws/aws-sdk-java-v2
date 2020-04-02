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
import static org.junit.Assert.assertNotNull;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.function.Consumer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.AccessControlPolicy;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class AclIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(AclIntegrationTest.class);

    private static final String KEY = "some-key";

    @BeforeClass
    public static void setupFixture() {
        createBucket(BUCKET);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), RequestBody.fromString("helloworld"));
    }

    @AfterClass
    public static void deleteAllBuckets() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void putGetObjectAcl() {
        GetObjectAclResponse objectAcl = s3.getObjectAcl(b -> b.bucket(BUCKET).key(KEY));
        GetObjectAclResponse objectAclAsyncResponse = s3Async.getObjectAcl(b -> b.bucket(BUCKET).key(KEY)).join();
        assertThat(objectAcl.equalsBySdkFields(objectAclAsyncResponse)).isTrue();
        Consumer<AccessControlPolicy.Builder> aclBuilder = a -> a.owner(objectAcl.owner())
                                                                 .grants(objectAcl.grants());


        assertNotNull(s3.putObjectAcl(b -> b.bucket(BUCKET)
                                            .key(KEY)
                                            .accessControlPolicy(aclBuilder)));

        assertNotNull(s3Async.putObjectAcl(b -> b.bucket(BUCKET)
                                                 .key(KEY)
                                                 .accessControlPolicy(aclBuilder)).join());
    }

    @Test
    public void putGetBucketAcl() {
        GetBucketAclResponse bucketAcl = s3.getBucketAcl(b -> b.bucket(BUCKET));
        GetBucketAclResponse bucketAclAsyncResponse = s3Async.getBucketAcl(b -> b.bucket(BUCKET)).join();
        assertThat(bucketAcl.equalsBySdkFields(bucketAclAsyncResponse)).isTrue();
        Consumer<AccessControlPolicy.Builder> aclBuilder = a -> a.owner(bucketAcl.owner())
                                                                 .grants(bucketAcl.grants());
        assertNotNull(s3.putBucketAcl(b -> b.bucket(BUCKET)
                                            .accessControlPolicy(aclBuilder)));
        assertNotNull(s3Async.putBucketAcl(b -> b.bucket(BUCKET)
                                                 .accessControlPolicy(aclBuilder)).join());

    }
}
