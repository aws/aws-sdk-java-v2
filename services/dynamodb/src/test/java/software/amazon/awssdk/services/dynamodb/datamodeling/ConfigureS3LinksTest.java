/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.regions.Region;

public class ConfigureS3LinksTest {

    private S3ClientCache s3cc;

    @Before
    public void setUp() throws Exception {
        s3cc = new S3ClientCache(new AwsCredentials("mock", "mock"));
    }

    @Test
    public void testS3LinkWithStringRegion() {
        CorrectTestClass obj = new CorrectTestClass();
        S3Link s3 = new S3Link(s3cc, "ap-southeast-1", "nonexisting-test-bucketname2", "key");
        obj.setS3(s3);

        assertNotNull(obj.s3());
        assertEquals("nonexisting-test-bucketname2", obj.s3().bucketName());
        assertSame(Region.AP_SOUTHEAST_1.value(), obj.s3().s3Region().value());
        assertSame("ap-southeast-1", obj.s3().getRegion());
    }

    @Test
    public void testManyS3LinksClass() {
        ManyS3LinksTestClass obj = new ManyS3LinksTestClass();
        assertNull(obj.s31());
    }

    @DynamoDbTable(tableName = "nonexisting-test-tablename")
    public static class CorrectTestClass {

        private String hk;
        private S3Link s3;

        public CorrectTestClass() {
        }

        @DynamoDbHashKey
        public String getHk() {
            return hk;
        }

        public void setHk(String hk) {
            this.hk = hk;
        }

        public S3Link s3() {
            return s3;
        }

        public void setS3(S3Link s3) {
            this.s3 = s3;
        }
    }

    @DynamoDbTable(tableName = "nonexisting-test-tablename")
    public static class ManyS3LinksTestClass {

        private String hk;
        private S3Link s31;
        private S3Link s32;
        private S3Link s33;
        private S3Link s34;
        private S3Link s35;
        private S3Link s36;

        public ManyS3LinksTestClass() {
        }

        @DynamoDbHashKey
        public String getHk() {
            return hk;
        }

        public void setHk(String hk) {
            this.hk = hk;
        }

        public S3Link s31() {
            return s31;
        }

        public void setS31(S3Link s31) {
            this.s31 = s31;
        }

        public S3Link s32() {
            return s32;
        }

        public void setS32(S3Link s32) {
            this.s32 = s32;
        }

        public S3Link s33() {
            return s33;
        }

        public void setS33(S3Link s33) {
            this.s33 = s33;
        }

        public S3Link s34() {
            return s34;
        }

        public void setS34(S3Link s34) {
            this.s34 = s34;
        }

        public S3Link s35() {
            return s35;
        }

        public void setS35(S3Link s35) {
            this.s35 = s35;
        }

        public S3Link s36() {
            return s36;
        }

        public void setS36(S3Link s36) {
            this.s36 = s36;
        }
    }
}
