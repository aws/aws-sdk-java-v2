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

package software.amazon.awssdk.services.dynamodb;

import static software.amazon.awssdk.core.config.SdkAdvancedClientOption.SIGNER;
import static software.amazon.awssdk.core.config.SdkAdvancedClientOption.SIGNER_CONTEXT;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsExecutionAttributes;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.internal.AwsSignerParams;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.signerspi.SignerContext;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.apache.ApacheSdkHttpClientFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.utils.IoUtils;

// TODO Clean up test. Using hard coded values for initial testing
public class SignersIntegrationTest {

    private static final String EXPECTED_RESULT = "{\"Item\":{\"value\":{\"N\":\"5\"},\"UID\":{\"S\":\"varunkn\"},\"foo\":{\"S\":\"bar\"}}}";

    @Test
    public void test_UsingSdkDefaultSigners() throws JsonProcessingException {

        Region region = Region.US_WEST_1;

        DynamoDBClient client = DynamoDBClient.builder()
                                              .region(region)
                                              .build();

        Map<String, AttributeValue> item =
            client.getItem(GetItemRequest.builder()
                                         .tableName("VoxTests1")
                                         .key(Collections.singletonMap("UID", AttributeValue.builder()
                                                                                            .s("varunkn")
                                                                                            .build()))
                                         .build())
                  .item();

        Assert.assertEquals("varunkn", item.get("UID").s());
        Assert.assertEquals("bar", item.get("foo").s());
    }


    @Test
    public void test_PassingSignerConfig() {

        Region region = Region.US_WEST_1;

        DynamoDBClient client = DynamoDBClient.builder()
                                              .region(region)
                                              .overrideConfiguration(
                                                  ClientOverrideConfiguration.builder()
                                                                             .advancedOption(SIGNER, new Aws4Signer())
                                                                             .advancedOption(SIGNER_CONTEXT,
                                                                                             createSignerContext())
                                                                             .build())
                                              .build();

        Map<String, AttributeValue> item =
            client.getItem(GetItemRequest.builder()
                                         .tableName("VoxTests1")
                                         .key(Collections.singletonMap("UID", AttributeValue.builder()
                                                                                            .s("varunkn")
                                                                                            .build()))
                                         .build())
                  .item();

        Assert.assertEquals("varunkn", item.get("UID").s());
        Assert.assertEquals("bar", item.get("foo").s());
    }

    @Test
    public void test_WithoutUsingSdkClient() throws Exception {
        Aws4Signer signer = new Aws4Signer();
        SdkHttpFullRequest httpFullRequest = generateBasicRequest();

        SdkHttpFullRequest signedRequest = signer.sign(httpFullRequest, createSignerContext());

        ApacheSdkHttpClientFactory httpClientFactory = ApacheSdkHttpClientFactory.builder().build();
        SdkHttpClient httpClient = httpClientFactory.createHttpClient();

        SdkHttpFullResponse response = httpClient.prepareRequest(signedRequest, SdkRequestContext.builder().build())
                                                 .call();

        String str = IoUtils.toString(response.content().get());
        System.out.println(str);

        if (response.statusCode() != 200) {
            Assert.fail("Call did not succeed");
        }

        Assert.assertEquals(EXPECTED_RESULT, str);
    }

    private SignerContext createSignerContext() {
        AwsSignerParams signerParams = new AwsSignerParams();
        signerParams.setAwsCredentials(DefaultCredentialsProvider.create().getCredentials());
        signerParams.setSigningName("dynamodb");
        signerParams.setRegion(Region.US_WEST_1);

        SignerContext signerContext = new SignerContext();
        signerContext.putAttribute(AwsExecutionAttributes.AWS_SIGNER_PARAMS, signerParams);
        return signerContext;
    }

    private SdkHttpFullRequest generateBasicRequest() {
        final String content = "{\"TableName\":\"VoxTests1\",\"Key\":{\"UID\":{\"S\":\"varunkn\"}}}";
        final InputStream contentStream = new ByteArrayInputStream(content.getBytes());

        return SdkHttpFullRequest.builder()
                                 .content(contentStream)
                                 .method(SdkHttpMethod.POST)
                                 .header("Content-Length", Integer.toString(content.length()))
                                 .header("Content-Type", "application/x-amz-json-1.0")
                                 .header("X-Amz-Target", "DynamoDB_20120810.GetItem")
                                 .encodedPath("/")
                                 .protocol("https")
                                 .host("dynamodb.us-west-1.amazonaws.com")
                                 .build();
    }
}