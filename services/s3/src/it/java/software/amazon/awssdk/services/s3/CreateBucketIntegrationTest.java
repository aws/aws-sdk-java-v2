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
package software.amazon.awssdk.services.s3;

import org.junit.Test;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

public class CreateBucketIntegrationTest extends S3IntegrationTestBase {

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameIsLessThan3Characters() {
        s3.createBucket(CreateBucketRequest.builder().bucket("s3").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameIsGreaterThan63Characters() {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket("wajb5vwtlkhx4ow1t9e6l39rdy7amxxyttryfdw4y4nwomxervpti82lphi5plm8")
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameIsIpAddress() {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket("127.0.0.1")
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameContainsUpperCaseCharacters() {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket("UPPERCASE")
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameContainsWhiteSpace() {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket("white space")
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameBeginsWithPeriod() {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket(".period")
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameContainsAdjacentPeriods() {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket("..period")
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameBeginsWithADash() {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket("-dash")
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBucket_ThrowsIllegalArgumentException_WhenBucketNameHasDashAdjacentAPeriod() {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket("-.dashperiod")
                                           .build());
    }
}
