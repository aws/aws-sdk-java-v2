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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/**
 * Regression test for long DynamoDB attribute names. DynamoDB allows attribute names up to 65,535 bytes,
 * but Jackson's default maxNameLength of 50,000 caused deserialization failures for names exceeding that limit.
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html">
 *     DynamoDB Naming Rules</a>
 */
@WireMockTest
class LongAttributeNameTest {

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));

    private static final String LONG_ATTR_NAME;
    private static final String RESPONSE_BODY;

    static {
        char[] chars = new char[65_535];
        Arrays.fill(chars, 'a');
        LONG_ATTR_NAME = new String(chars);
        RESPONSE_BODY = "{\"Item\": {\"pk\": {\"S\": \"test1\"}, \"" + LONG_ATTR_NAME + "\": {\"S\": \"value\"}}}";
    }

    private void stubResponse(WireMockRuntimeInfo wmInfo) {
        wmInfo.getWireMock().register(any(urlEqualTo("/"))
            .willReturn(aResponse().withStatus(200).withBody(RESPONSE_BODY)));
    }

    @Test
    void syncClient_getItem_withLongAttributeName_succeeds(WireMockRuntimeInfo wmInfo) {
        stubResponse(wmInfo);

        DynamoDbClient client = DynamoDbClient.builder()
                                              .endpointOverride(URI.create(wmInfo.getHttpBaseUrl()))
                                              .region(Region.US_EAST_1)
                                              .credentialsProvider(CREDENTIALS)
                                              .build();

        GetItemResponse response = client.getItem(r -> r.tableName("test")
                                                         .key(Collections.singletonMap("pk",
                                                             AttributeValue.fromS("test1"))));

        assertThat(response.item()).containsKey(LONG_ATTR_NAME);
        assertThat(response.item().get(LONG_ATTR_NAME).s()).isEqualTo("value");
    }

    @Test
    void asyncClient_getItem_withLongAttributeName_succeeds(WireMockRuntimeInfo wmInfo) {
        stubResponse(wmInfo);

        DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                                                        .endpointOverride(URI.create(wmInfo.getHttpBaseUrl()))
                                                        .region(Region.US_EAST_1)
                                                        .credentialsProvider(CREDENTIALS)
                                                        .build();

        GetItemResponse response = client.getItem(r -> r.tableName("test")
                                                         .key(Collections.singletonMap("pk",
                                                             AttributeValue.fromS("test1")))).join();

        assertThat(response.item()).containsKey(LONG_ATTR_NAME);
        assertThat(response.item().get(LONG_ATTR_NAME).s()).isEqualTo("value");
    }
}
