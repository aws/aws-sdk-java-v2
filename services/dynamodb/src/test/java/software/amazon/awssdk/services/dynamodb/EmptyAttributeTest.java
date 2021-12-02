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

package software.amazon.awssdk.services.dynamodb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetRecordsResponse;
import software.amazon.awssdk.services.dynamodb.model.StreamRecord;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClient;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;

public class EmptyAttributeTest {
    private static final AttributeValue EMPTY_STRING = AttributeValue.builder().s("").build();
    private static final AttributeValue EMPTY_BINARY =
        AttributeValue.builder().b(SdkBytes.fromByteArray(new byte[]{})).build();

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private DynamoDbClient dynamoDbClient;
    private DynamoDbAsyncClient dynamoDbAsyncClient;
    private DynamoDbStreamsClient dynamoDbStreamsClient;
    private DynamoDbStreamsAsyncClient dynamoDbStreamsAsyncClient;

    @Before
    public void setup() {
        dynamoDbClient =
            DynamoDbClient.builder()
                          .credentialsProvider(
                              StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                          .region(Region.US_WEST_2)
                          .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                          .build();

        dynamoDbAsyncClient =
            DynamoDbAsyncClient.builder()
                               .credentialsProvider(
                                   StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                               .region(Region.US_WEST_2)
                               .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                               .build();

        dynamoDbStreamsClient =
            DynamoDbStreamsClient.builder()
                                 .credentialsProvider(
                                     StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                                 .region(Region.US_WEST_2)
                                 .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                 .build();

        dynamoDbStreamsAsyncClient =
            DynamoDbStreamsAsyncClient.builder()
                                 .credentialsProvider(
                                     StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                                 .region(Region.US_WEST_2)
                                 .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                 .build();    }

    @Test
    public void syncClient_getItem_emptyString() {
        stubFor(any(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(200).withBody("{\"Item\": {\"attribute\": {\"S\": \"\"}}}")));

        GetItemResponse response = dynamoDbClient.getItem(r -> r.tableName("test"));
        assertThat(response.item()).containsKey("attribute");
        AttributeValue attributeValue = response.item().get("attribute");
        assertThat(attributeValue.s()).isEmpty();
    }

    @Test
    public void asyncClient_getItem_emptyString() {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse().withStatus(200).withBody("{\"Item\": {\"attribute\": {\"S\": \"\"}}}")));

        GetItemResponse response = dynamoDbAsyncClient.getItem(r -> r.tableName("test")).join();
        assertThat(response.item()).containsKey("attribute");
        AttributeValue attributeValue = response.item().get("attribute");
        assertThat(attributeValue.s()).isEmpty();
    }

    @Test
    public void syncClient_getItem_emptyBinary() {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse().withStatus(200).withBody("{\"Item\": {\"attribute\": {\"B\": \"\"}}}")));

        GetItemResponse response = dynamoDbClient.getItem(r -> r.tableName("test"));
        assertThat(response.item()).containsKey("attribute");
        AttributeValue attributeValue = response.item().get("attribute");
        assertThat(attributeValue.b().asByteArray()).isEmpty();
    }

    @Test
    public void asyncClient_getItem_emptyBinary() {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse().withStatus(200).withBody("{\"Item\": {\"attribute\": {\"B\": \"\"}}}")));

        GetItemResponse response = dynamoDbAsyncClient.getItem(r -> r.tableName("test")).join();
        assertThat(response.item()).containsKey("attribute");
        AttributeValue attributeValue = response.item().get("attribute");
        assertThat(attributeValue.b().asByteArray()).isEmpty();
    }

    @Test
    public void syncClient_putItem_emptyString() {
        stubFor(any(urlEqualTo("/")).willReturn((aResponse().withStatus(200))));

        dynamoDbClient.putItem(r -> r.item(Collections.singletonMap("stringAttribute", EMPTY_STRING)));

        verify(postRequestedFor(urlEqualTo("/"))
                   .withRequestBody(equalTo("{\"Item\":{\"stringAttribute\":{\"S\":\"\"}}}")));
    }

    @Test
    public void asyncClient_putItem_emptyString() {
        stubFor(any(urlEqualTo("/")).willReturn((aResponse().withStatus(200))));

        dynamoDbAsyncClient.putItem(r -> r.item(Collections.singletonMap("stringAttribute", EMPTY_STRING))).join();

        verify(postRequestedFor(urlEqualTo("/"))
                   .withRequestBody(equalTo("{\"Item\":{\"stringAttribute\":{\"S\":\"\"}}}")));
    }

    @Test
    public void syncClient_putItem_emptyBinary() {
        stubFor(any(urlEqualTo("/")).willReturn((aResponse().withStatus(200))));

        dynamoDbClient.putItem(r -> r.item(Collections.singletonMap("binaryAttribute", EMPTY_BINARY)));

        verify(postRequestedFor(urlEqualTo("/"))
                   .withRequestBody(equalTo("{\"Item\":{\"binaryAttribute\":{\"B\":\"\"}}}")));
    }

    @Test
    public void asyncClient_putItem_emptyStrring() {
        stubFor(any(urlEqualTo("/")).willReturn((aResponse().withStatus(200))));

        dynamoDbAsyncClient.putItem(r -> r.item(Collections.singletonMap("binaryAttribute", EMPTY_BINARY))).join();

        verify(postRequestedFor(urlEqualTo("/"))
                   .withRequestBody(equalTo("{\"Item\":{\"binaryAttribute\":{\"B\":\"\"}}}")));
    }

    @Test
    public void syncClient_getRecords_emptyString() {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse().withStatus(200).withBody(
                        "{" +
                        "  \"NextShardIterator\": \"arn:aws:dynamodb:us-west-2:111122223333:table/Forum/stream/2015-05-20T20:51:10.252|1|AAAAAAAAAAGQBYshYDEe\",\n" +
                        "  \"Records\": [\n" +
                        "    {\n" +
                        "      \"awsRegion\": \"us-west-2\",\n" +
                        "      \"dynamodb\": {\n" +
                        "        \"ApproximateCreationDateTime\": 1.46480431E9,\n" +
                        "        \"Keys\": {\n" +
                        "          \"stringKey\": {\"S\": \"DynamoDB\"}\n" +
                        "        },\n" +
                        "        \"NewImage\": {\n" +
                        "          \"stringAttribute\": {\"S\": \"\"}\n" +
                        "        },\n" +
                        "        \"OldImage\": {\n" +
                        "          \"stringAttribute\": {\"S\": \"\"}\n" +
                        "        },\n" +
                        "        \"SequenceNumber\": \"300000000000000499659\",\n" +
                        "        \"SizeBytes\": 41,\n" +
                        "        \"StreamViewType\": \"NEW_AND_OLD_IMAGES\"\n" +
                        "      },\n" +
                        "      \"eventID\": \"e2fd9c34eff2d779b297b26f5fef4206\",\n" +
                        "      \"eventName\": \"INSERT\",\n" +
                        "      \"eventSource\": \"aws:dynamodb\",\n" +
                        "      \"eventVersion\": \"1.0\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}")));

        GetRecordsResponse response = dynamoDbStreamsClient.getRecords(r -> r.shardIterator("test"));

        assertThat(response.records()).hasSize(1);
        StreamRecord record = response.records().get(0).dynamodb();
        assertThat(record.oldImage()).containsEntry("stringAttribute", EMPTY_STRING);
        assertThat(record.newImage()).containsEntry("stringAttribute", EMPTY_STRING);
    }

    @Test
    public void asyncClient_getRecords_emptyString() {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse().withStatus(200).withBody(
                        "{" +
                            "  \"NextShardIterator\": \"arn:aws:dynamodb:us-west-2:111122223333:table/Forum/stream/2015-05-20T20:51:10.252|1|AAAAAAAAAAGQBYshYDEe\",\n" +
                            "  \"Records\": [\n" +
                            "    {\n" +
                            "      \"awsRegion\": \"us-west-2\",\n" +
                            "      \"dynamodb\": {\n" +
                            "        \"ApproximateCreationDateTime\": 1.46480431E9,\n" +
                            "        \"Keys\": {\n" +
                            "          \"stringKey\": {\"S\": \"DynamoDB\"}\n" +
                            "        },\n" +
                            "        \"NewImage\": {\n" +
                            "          \"stringAttribute\": {\"S\": \"\"}\n" +
                            "        },\n" +
                            "        \"OldImage\": {\n" +
                            "          \"stringAttribute\": {\"S\": \"\"}\n" +
                            "        },\n" +
                            "        \"SequenceNumber\": \"300000000000000499659\",\n" +
                            "        \"SizeBytes\": 41,\n" +
                            "        \"StreamViewType\": \"NEW_AND_OLD_IMAGES\"\n" +
                            "      },\n" +
                            "      \"eventID\": \"e2fd9c34eff2d779b297b26f5fef4206\",\n" +
                            "      \"eventName\": \"INSERT\",\n" +
                            "      \"eventSource\": \"aws:dynamodb\",\n" +
                            "      \"eventVersion\": \"1.0\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}")));

        GetRecordsResponse response = dynamoDbStreamsAsyncClient.getRecords(r -> r.shardIterator("test")).join();

        assertThat(response.records()).hasSize(1);
        StreamRecord record = response.records().get(0).dynamodb();
        assertThat(record.oldImage()).containsEntry("stringAttribute", EMPTY_STRING);
        assertThat(record.newImage()).containsEntry("stringAttribute", EMPTY_STRING);
    }

    @Test
    public void syncClient_getRecords_emptyBinary() {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse().withStatus(200).withBody(
                        "{" +
                            "  \"NextShardIterator\": \"arn:aws:dynamodb:us-west-2:111122223333:table/Forum/stream/2015-05-20T20:51:10.252|1|AAAAAAAAAAGQBYshYDEe\",\n" +
                            "  \"Records\": [\n" +
                            "    {\n" +
                            "      \"awsRegion\": \"us-west-2\",\n" +
                            "      \"dynamodb\": {\n" +
                            "        \"ApproximateCreationDateTime\": 1.46480431E9,\n" +
                            "        \"Keys\": {\n" +
                            "          \"stringKey\": {\"S\": \"DynamoDB\"}\n" +
                            "        },\n" +
                            "        \"NewImage\": {\n" +
                            "          \"binaryAttribute\": {\"B\": \"\"}\n" +
                            "        },\n" +
                            "        \"OldImage\": {\n" +
                            "          \"binaryAttribute\": {\"B\": \"\"}\n" +
                            "        },\n" +
                            "        \"SequenceNumber\": \"300000000000000499659\",\n" +
                            "        \"SizeBytes\": 41,\n" +
                            "        \"StreamViewType\": \"NEW_AND_OLD_IMAGES\"\n" +
                            "      },\n" +
                            "      \"eventID\": \"e2fd9c34eff2d779b297b26f5fef4206\",\n" +
                            "      \"eventName\": \"INSERT\",\n" +
                            "      \"eventSource\": \"aws:dynamodb\",\n" +
                            "      \"eventVersion\": \"1.0\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}")));

        GetRecordsResponse response = dynamoDbStreamsClient.getRecords(r -> r.shardIterator("test"));

        assertThat(response.records()).hasSize(1);
        StreamRecord record = response.records().get(0).dynamodb();
        assertThat(record.oldImage()).containsEntry("binaryAttribute", EMPTY_BINARY);
        assertThat(record.newImage()).containsEntry("binaryAttribute", EMPTY_BINARY);
    }

    @Test
    public void asyncClient_getRecords_emptyBinary() {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse().withStatus(200).withBody(
                        "{" +
                            "  \"NextShardIterator\": \"arn:aws:dynamodb:us-west-2:111122223333:table/Forum/stream/2015-05-20T20:51:10.252|1|AAAAAAAAAAGQBYshYDEe\",\n" +
                            "  \"Records\": [\n" +
                            "    {\n" +
                            "      \"awsRegion\": \"us-west-2\",\n" +
                            "      \"dynamodb\": {\n" +
                            "        \"ApproximateCreationDateTime\": 1.46480431E9,\n" +
                            "        \"Keys\": {\n" +
                            "          \"stringKey\": {\"S\": \"DynamoDB\"}\n" +
                            "        },\n" +
                            "        \"NewImage\": {\n" +
                            "          \"binaryAttribute\": {\"B\": \"\"}\n" +
                            "        },\n" +
                            "        \"OldImage\": {\n" +
                            "          \"binaryAttribute\": {\"B\": \"\"}\n" +
                            "        },\n" +
                            "        \"SequenceNumber\": \"300000000000000499659\",\n" +
                            "        \"SizeBytes\": 41,\n" +
                            "        \"StreamViewType\": \"NEW_AND_OLD_IMAGES\"\n" +
                            "      },\n" +
                            "      \"eventID\": \"e2fd9c34eff2d779b297b26f5fef4206\",\n" +
                            "      \"eventName\": \"INSERT\",\n" +
                            "      \"eventSource\": \"aws:dynamodb\",\n" +
                            "      \"eventVersion\": \"1.0\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}")));

        GetRecordsResponse response = dynamoDbStreamsAsyncClient.getRecords(r -> r.shardIterator("test")).join();

        assertThat(response.records()).hasSize(1);
        StreamRecord record = response.records().get(0).dynamodb();
        assertThat(record.oldImage()).containsEntry("binaryAttribute", EMPTY_BINARY);
        assertThat(record.newImage()).containsEntry("binaryAttribute", EMPTY_BINARY);
    }
}
