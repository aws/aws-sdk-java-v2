/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.module.tests;

import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Class javadoc
 */
public class S3Test {

    private S3Client s3Client = S3Client.builder().httpClient(UrlConnectionHttpClient.builder().build())
                                        //.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                        //.region(Region.US_WEST_2)
                                        .build();
    private S3Client s3Client1 = S3Client.builder().httpClient(ApacheHttpClient.builder().build())
                                         //.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                         //.region(Region.US_WEST_2)
                                         .build();


    public void callS3client() {
        s3Client1.headBucket(b -> b.bucket("test-bucket-zoe-2"));
    }
//
//    public void callS3ClientAync() {
//        S3AsyncClient client = S3AsyncClient.create();
//        client.headBucket(b -> b.bucket("test-bucket-zoe-2")).join();
//    }

    public static void main(String... args) {
        S3Test s3Test = new S3Test();
        s3Test.callS3client();
    }
}
