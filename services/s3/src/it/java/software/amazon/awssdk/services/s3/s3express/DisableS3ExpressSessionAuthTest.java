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

package software.amazon.awssdk.services.s3.s3express;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import static com.github.tomakehurst.wiremock.client.WireMock.*;


public class DisableS3ExpressSessionAuthTest extends S3ExpressIntegrationTestBase {

    @RegisterExtension
    static WireMockExtension s3WireMock = WireMockExtension.newInstance()
                                                           .options(wireMockConfig().dynamicPort())
                                                           .build();

    private static final Region TEST_REGION = Region.US_EAST_1;
    private static final String AZ = "use1-az4";
    private static S3AsyncClient s3CrtAsync;
    private static S3AsyncClient s3CrtAsyncDefault;
    private static String testBucket;

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    private static final String S3EXPRESS_BUCKET_PATTERN = temporaryBucketName(S3ExpressIntegrationTest.class) +"--%s--x-s3";


    private static String getS3ExpressBucketNameForAz(String az) {
        return String.format(S3EXPRESS_BUCKET_PATTERN, az);
    }

    private static final String CREATE_SESSION_RESPONSE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<CreateSessionResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n"
        + "<Credentials><SessionToken>TheToken</SessionToken><SecretAccessKey>TheSecret"
        + "</SecretAccessKey><AccessKeyId>TheAccessKey</AccessKeyId><Expiration>2025-02-19T00:23:59Z</Expiration></Credentials>\n"
        + "</CreateSessionResult>";

    @BeforeAll
    static void setup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_S3_DISABLE_EXPRESS_SESSION_AUTH.environmentVariable(), "false");

        testBucket = getS3ExpressBucketNameForAz(AZ);

        s3CrtAsyncDefault = s3CrtAsyncClientBuilder(TEST_REGION).endpointOverride(URI.create("http://s3.localhost.localstack.cloud:" + s3WireMock.getPort())).build();
        s3CrtAsync = s3CrtAsyncClientBuilder(TEST_REGION).disableS3ExpressSessionAuth(true)
                                                         .endpointOverride(URI.create("http://s3.localhost.localstack.cloud:" + s3WireMock.getPort())).build();

    }

    @AfterAll
    static void clear() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        s3CrtAsync.close();
        s3CrtAsyncDefault.close();
    }

    private void setupWireMockStub() {
        s3WireMock.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(CREATE_SESSION_RESPONSE)));
    }


    @Test
    void defaultS3CrtAsyncClient_useS3ExpressAuth() {
        //x-amz-s3session-token will be there
        setupWireMockStub();
        s3CrtAsyncDefault.listObjectsV2(r -> r.bucket(testBucket)).join();
        s3WireMock.verify(1,getRequestedFor(urlPathMatching(".*"))
                  .withHeader("x-amz-s3session-token", matching(".*")));
    }


    @Test
    void disabledS3CrtAsyncClient_NotUseS3ExpressAuth() {
        //x-amz-s3session-token will not be there
        setupWireMockStub();
        s3CrtAsync.listObjectsV2(r -> r.bucket(testBucket)).join();
        s3WireMock.verify(1,getRequestedFor(urlPathMatching(".*"))
            .withoutHeader("x-amz-s3session-token"));
    }
}
