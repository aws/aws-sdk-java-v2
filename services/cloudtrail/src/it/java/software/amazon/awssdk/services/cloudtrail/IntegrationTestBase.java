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

package software.amazon.awssdk.services.cloudtrail;

import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class IntegrationTestBase extends AwsIntegrationTestBase {

    protected static CloudTrailClient cloudTrail;
    protected static S3Client s3;
    protected static Region region = Region.US_WEST_2;

    @BeforeClass
    public static void setUp() throws IOException {
        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");
        cloudTrail = CloudTrailClient.builder().credentialsProvider(StaticCredentialsProvider.create(getCredentials())).build();
        s3 = S3Client.builder()
                     .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                     .region(region)
                     .build();
    }
}