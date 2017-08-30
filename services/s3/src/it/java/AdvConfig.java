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

import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.auth.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AdvancedConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.sync.RequestBody;

public class AdvConfig {

    public static void main(String[] args) {
        BasicConfigurator.configure();
        S3Client.builder()
                .advancedConfiguration(S3AdvancedConfiguration.builder()
                                                              .accelerateModeEnabled(true)
                                                              .build())
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.builder().profileName("personal").build())
                .build()
                .putObject(PutObjectRequest.builder()
                                           .bucket("shorea-public")
                                           .key("accelerate-key")
                                           .build(),
                           RequestBody.of("content"));

    }
}
