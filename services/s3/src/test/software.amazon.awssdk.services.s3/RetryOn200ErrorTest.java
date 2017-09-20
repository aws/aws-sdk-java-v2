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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class RetryOn200ErrorTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(1341);

    private S3Client client;

    @Before
    public void setup() {
        BasicConfigurator.configure();
        client = S3Client.builder()
                         .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                         .region(Region.US_EAST_1)
                         .credentialsProvider(new StaticCredentialsProvider(new AwsCredentials("akid", "skid")))
                         .build();
    }

    @Test
    public void copy_Retries3Times_When200WithError() {

        final String errorResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>Internal Error</Code><Message>Please try again.</Message><RequestId>36E5C81B8463E101</RequestId><HostId>FJKdbo9Vbfb+MGbciAgKQ+Dy8mQ70rKNaz7PHvoCNKiZuh0OcKJd9Y9a6g8v1Oec</HostId></Error>";
        stubFor(put(urlEqualTo("/test/testkey")).willReturn(aResponse().withStatus(200).withBody(errorResponse)));

        try {
            client.copyObject(CopyObjectRequest.builder().bucket("test").key("testkey").copySource("test1/test1").build());
        } catch (Exception e) {
            wireMock.verify(4, putRequestedFor(urlEqualTo("/test/testkey")));
            throw e;
        }
    }

    @Test
    public void copy_Retries1Time_WhenSecondRequestSucceeds() {
        stubErrorFirstTime();

        client.copyObject(CopyObjectRequest.builder().bucket("test").key("testkey").copySource("test1/test1").build());
        wireMock.verify(2, putRequestedFor(urlEqualTo("/test/testkey")));
    }

    private void stubErrorFirstTime() {
        final String errorResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>Internal Error</Code><Message>Please try again.</Message><RequestId>36E5C81B8463E101</RequestId><HostId>FJKdbo9Vbfb+MGbciAgKQ+Dy8mQ70rKNaz7PHvoCNKiZuh0OcKJd9Y9a6g8v1Oec</HostId></Error>";
        final String successResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CopyObjectResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><LastModified>2016-07-26T21:45:59.000Z</LastModified><ETag>\"a8c15d6fb93ee5f088adb1d184366038\"</ETag></CopyObjectResult>\n";

        stubFor(put(urlMatching("/test/testkey")).inScenario("FailFailSucceed")
                                                 .whenScenarioStateIs(STARTED)
                                                 .willReturn(aResponse().withStatus(200)
                                                                        .withBody(errorResponse))
                                                 .willSetStateTo("FirstRequest"));

        stubFor(put(urlMatching("/test/testkey")).inScenario("FailFailSucceed")
                                                 .whenScenarioStateIs("FirstRequest")
                                                 .willReturn(aResponse().withStatus(200)
                                                                        .withBody(successResponse)));
    }
}
