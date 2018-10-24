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

//import static org.junit.jupiter.api.condition.JRE.JAVA_10;
//import static org.junit.jupiter.api.condition.JRE.JAVA_9;

import org.junit.Test;
import software.amazon.awssdk.testutils.service.AwsTestBase;

//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.condition.EnabledOnJre;

/**
 * Class javadoc
 */
//@EnabledOnJre( {JAVA_9, JAVA_10})
public class ApacheClientTest extends AwsTestBase {

//    private S3Client s3Client = S3Client.builder()
//                                        //.region(Region.US_WEST_2)
//                                        .httpClient(UrlConnectionHttpClient.builder().build())
////                                        .build();
// private S3Client s3Client = S3Client.builder().httpClient(UrlConnectionHttpClient.builder().build())
//                                     //.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
//                                     //.region(Region.US_WEST_2)
//                                     .build();
//    private S3Client s3Client1 = S3Client.builder().httpClient(ApacheHttpClient.builder().build())
//                                         //.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
//                                         //.region(Region.US_WEST_2)
//                                         .build();
//
//    private S3AsyncClient asyncClient = S3AsyncClient.create();
//
////    @Rule
////    public WireMockRule wireMock = new WireMockRule(WireMockConfiguration.wireMockConfig()
////                                                                         .port(0));
////
//////    @BeforeEach
////    @Before
////    public void setup() {
////        client = ProtocolRestJsonClient.builder()
////                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
////                                                                                                                        "skid")))
////                                       .region(Region.US_EAST_1)
////                                       .endpointOverride(URI.create("https://localhost:" + wireMock.port()))
////                                       .build();
////
////        asyncClient = ProtocolRestJsonAsyncClient.builder()
////                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
////                                                                                                                        "skid")))
////                                       .region(Region.US_EAST_1)
////                                       .endpointOverride(URI.create("https://localhost:" + wireMock.port()))
////                                       .httpClient(NettyNioAsyncHttpClient.builder().build())
////                                       .build();
////    }
////
////
////    @Test
////    public void test() {
////        client.allTypes();
////        asyncClient.allTypes().join();
////    }
//
//    @Test
//    public void test() {
//        s3Client1.headBucket(b -> b.bucket("test-bucket-zoe-2"));
//        asyncClient.headBucket(b -> b.bucket("test-bucket-zoe-2")).join();
//    }

    @Test
    public void test() {
//        S3Test s3Test = new S3Test();
//
//        s3Test.callS3client();
//
//        s3Test.callS3ClientAync();

        S3Test.main();
    }
}
