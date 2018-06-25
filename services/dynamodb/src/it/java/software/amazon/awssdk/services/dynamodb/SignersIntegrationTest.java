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

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.internal.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.utils.IoUtils;
import utils.resources.tables.BasicTempTable;
import utils.test.util.DynamoDBTestBase;
import utils.test.util.TableUtils;

public class SignersIntegrationTest extends DynamoDBTestBase {

    private static final String TABLE_NAME = BasicTempTable.TEMP_TABLE_NAME;
    private static final String HASH_KEY_NAME = BasicTempTable.HASH_KEY_NAME;
    private static final String HASH_KEY_VALUE = "123789";
    private static final String ATTRIBUTE_FOO = "foo";
    private static final String ATTRIBUTE_FOO_VALUE = "bar";
    private static final AwsCredentials awsCredentials = CREDENTIALS_PROVIDER_CHAIN.resolveCredentials();
    private static final String SIGNING_NAME = "dynamodb";

    @BeforeClass
    public static void setUpFixture() throws Exception {
        DynamoDBTestBase.setUpTestBase();

        dynamo.createTable(CreateTableRequest.builder().tableName(TABLE_NAME)
                                             .keySchema(KeySchemaElement.builder().keyType(KeyType.HASH)
                                                                        .attributeName(HASH_KEY_NAME)
                                                                        .build())
                                             .attributeDefinitions(AttributeDefinition.builder()
                                                                                      .attributeType(ScalarAttributeType.S)
                                                                                      .attributeName(HASH_KEY_NAME)
                                                                                      .build())
                                             .provisionedThroughput(ProvisionedThroughput.builder()
                                                                                         .readCapacityUnits(5L)
                                                                                         .writeCapacityUnits(5L)
                                                                                         .build())
                                             .build());

        TableUtils.waitUntilActive(dynamo, TABLE_NAME);

        putTestData();
    }

    private static void putTestData() {
        Map<String, AttributeValue> item = new HashMap();
        item.put(HASH_KEY_NAME, AttributeValue.builder().s(HASH_KEY_VALUE).build());
        item.put(ATTRIBUTE_FOO, AttributeValue.builder().s(ATTRIBUTE_FOO_VALUE).build());
        dynamo.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
    }

    @AfterClass
    public static void cleanUpFixture() {
        dynamo.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
    }

    @Test
    public void test_UsingSdkDefaultClient() throws JsonProcessingException {
        getItemAndAssertValues(dynamo);
    }


    @Test
    public void test_UsingSdkClient_WithSignerSetInConfig() {
        DynamoDbClient client = getClientBuilder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                              .advancedOption(SIGNER, Aws4Signer.create())
                                                              .build())
            .build();

        getItemAndAssertValues(client);
    }

    @Test
    public void sign_WithoutUsingSdkClient_ThroughExecutionAttributes() throws Exception {
        Aws4Signer signer = Aws4Signer.create();
        SdkHttpFullRequest httpFullRequest = generateBasicRequest();

        // sign the request
        SdkHttpFullRequest signedRequest = signer.sign(httpFullRequest, constructExecutionAttributes());

        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        SdkHttpFullResponse response = httpClient.prepareRequest(signedRequest, SdkRequestContext.builder().build())
                                                 .call();

        assertEquals("Non success http status code", 200, response.statusCode());

        String actualResult = IoUtils.toString(response.content().get());
        assertEquals(getExpectedResult(), actualResult);
    }

    @Test
    public void test_SignMethod_WithModeledParam_And_WithoutUsingSdkClient() throws Exception {
        Aws4Signer signer = Aws4Signer.create();
        SdkHttpFullRequest httpFullRequest = generateBasicRequest();

        // sign the request
        SdkHttpFullRequest signedRequest = signer.sign(httpFullRequest, constructSignerParams());

        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        SdkHttpFullResponse response = httpClient.prepareRequest(signedRequest, SdkRequestContext.builder().build())
                                                 .call();

        assertEquals("Non success http status code", 200, response.statusCode());

        String actualResult = IoUtils.toString(response.content().get());
        assertEquals(getExpectedResult(), actualResult);
    }

    private SdkHttpFullRequest generateBasicRequest() {
        final String content = getInputContent();
        final InputStream contentStream = new ByteArrayInputStream(content.getBytes());

        return SdkHttpFullRequest.builder()
                                 .content(contentStream)
                                 .method(SdkHttpMethod.POST)
                                 .header("Content-Length", Integer.toString(content.length()))
                                 .header("Content-Type", "application/x-amz-json-1.0")
                                 .header("X-Amz-Target", "DynamoDB_20120810.GetItem")
                                 .encodedPath("/")
                                 .protocol("https")
                                 .host(getHost())
                                 .build();
    }

    private String getHost() {
        return String.format("dynamodb.%s.amazonaws.com", REGION.id());
    }

    private String getInputContent() {
        return "{  \n"
               + "   \"TableName\":\"" + TABLE_NAME + "\",\n"
               + "   \"Key\":{  \n"
               + "      \"" + HASH_KEY_NAME + "\":{  \n"
               + "         \"S\":\"" + HASH_KEY_VALUE + "\"\n"
               + "      }\n"
               + "   }\n"
               + "}".trim();
    }

    private String getExpectedResult() {
        String result =
                 "{"
               + "   \"Item\":{"
               + "      \"" + HASH_KEY_NAME + "\":{"
               + "         \"S\":\"" + HASH_KEY_VALUE + "\""
               + "      },"
               + "      \"" + ATTRIBUTE_FOO + "\":{"
               + "         \"S\":\"" + ATTRIBUTE_FOO_VALUE + "\""
               + "      }"
               + "   }"
               + "}";

        return result.replaceAll("\\s", "");
    }

    private void getItemAndAssertValues(DynamoDbClient client) {
        Map<String, AttributeValue> item =
            client.getItem(GetItemRequest.builder()
                                         .tableName(TABLE_NAME)
                                         .key(Collections.singletonMap(HASH_KEY_NAME, AttributeValue.builder()
                                                                                                    .s(HASH_KEY_VALUE)
                                                                                                    .build()))
                                         .build())
                  .item();

        assertEquals(HASH_KEY_VALUE, item.get(HASH_KEY_NAME).s());
        assertEquals(ATTRIBUTE_FOO_VALUE, item.get(ATTRIBUTE_FOO).s());
    }

    private Aws4SignerParams constructSignerParams() {
        return Aws4SignerParams.builder()
                               .awsCredentials(awsCredentials)
                               .signingName(SIGNING_NAME)
                               .signingRegion(REGION)
                               .build();
    }

    private ExecutionAttributes constructExecutionAttributes() {
        return new ExecutionAttributes()
            .putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, awsCredentials)
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, SIGNING_NAME)
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, REGION)
            .putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, Boolean.TRUE);
    }

    private static DynamoDbClientBuilder getClientBuilder() {
        return DynamoDbClient.builder()
                             .region(REGION)
                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);
    }
}