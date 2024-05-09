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

package software.amazon.awssdk.services.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.internal.MessageMD5ChecksumInterceptor;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Verifies that the logic in {@link MessageMD5ChecksumInterceptor} can be disabled, which is needed for use cases like
 * FIPS cryptography libraries that don't have MD5 support. SendMessage is used as the test API, but the flow is the
 * same for ReceiveMessage and SendMessageBatch.
 */
public class MessageMD5ChecksumValidationDisableTest {
    private static final AwsBasicCredentials CLIENT_CREDENTIALS = AwsBasicCredentials.create("ca", "cs");
    private static final Region CLIENT_REGION = Region.US_WEST_2;
    private static final String MESSAGE_ID = "0f433476-621e-4638-811a-112d2c2e41d7";

    private MockAsyncHttpClient asyncHttpClient;
    private MockSyncHttpClient syncHttpClient;

    @BeforeEach
    public void setupClient() {
        asyncHttpClient = new MockAsyncHttpClient();
        syncHttpClient = new MockSyncHttpClient();
    }

    @AfterEach
    public void cleanup() {
        asyncHttpClient.reset();
        syncHttpClient.reset();
    }

    @Test
    public void md5ValidationEnabled_default_md5InResponse_Works() {
        asyncHttpClient.stubResponses(responseWithMd5());
        SqsAsyncClient client = SqsAsyncClient.builder()
                                              .credentialsProvider(StaticCredentialsProvider.create(CLIENT_CREDENTIALS))
                                              .region(CLIENT_REGION)
                                              .httpClient(asyncHttpClient)
                                              .build();

        SendMessageResponse sendMessageResponse =
            client.sendMessage(r -> r.messageBody(messageBody()).messageAttributes(createAttributeValues())).join();

        assertThat(sendMessageResponse.messageId()).isEqualTo(MESSAGE_ID);
    }

    @Test
    public void md5ValidationEnabled_default_noMd5InResponse_throwsException() {
        asyncHttpClient.stubResponses(responseWithoutMd5());
        SqsAsyncClient client = SqsAsyncClient.builder()
                                              .credentialsProvider(StaticCredentialsProvider.create(CLIENT_CREDENTIALS))
                                              .region(CLIENT_REGION)
                                              .httpClient(asyncHttpClient)
                                              .build();

        assertThatThrownBy(() -> client.sendMessage(r -> r.messageBody(messageBody())
                                                          .messageAttributes(createAttributeValues()))
                                       .join())
            .hasMessageContaining("MD5 returned by SQS does not match the calculation on the original request");
    }

    @Test
    public void md5ValidationDisabled_md5InResponse_Works() {
        asyncHttpClient.stubResponses(responseWithMd5());
        SqsAsyncClient client = SqsAsyncClient.builder()
                                              .credentialsProvider(StaticCredentialsProvider.create(CLIENT_CREDENTIALS))
                                              .region(CLIENT_REGION)
                                              .httpClient(asyncHttpClient)
                                              .checksumValidationEnabled(false)
                                              .build();

        SendMessageResponse sendMessageResponse =
            client.sendMessage(r -> r.messageBody(messageBody()).messageAttributes(createAttributeValues())).join();

        assertThat(sendMessageResponse.messageId()).isEqualTo(MESSAGE_ID);
    }

    @Test
    public void md5ValidationDisabled_noMd5InResponse_Works() {
        asyncHttpClient.stubResponses(responseWithoutMd5());
        SqsAsyncClient client = SqsAsyncClient.builder()
                                              .credentialsProvider(StaticCredentialsProvider.create(CLIENT_CREDENTIALS))
                                              .region(CLIENT_REGION)
                                              .httpClient(asyncHttpClient)
                                              .checksumValidationEnabled(false)
                                              .build();

        SendMessageResponse sendMessageResponse =
            client.sendMessage(r -> r.messageBody(messageBody()).messageAttributes(createAttributeValues())).join();

        assertThat(sendMessageResponse.messageId()).isEqualTo(MESSAGE_ID);
    }

    @Test
    public void sync_md5ValidationDisabled_noMd5InResponse_Works() {
        syncHttpClient.stubResponses(responseWithoutMd5());
        SqsClient client = SqsClient.builder()
                                    .credentialsProvider(StaticCredentialsProvider.create(CLIENT_CREDENTIALS))
                                    .region(CLIENT_REGION)
                                    .httpClient(syncHttpClient)
                                    .checksumValidationEnabled(false)
                                    .build();

        SendMessageResponse sendMessageResponse =
            client.sendMessage(r -> r.messageBody(messageBody()).messageAttributes(createAttributeValues()));

        assertThat(sendMessageResponse.messageId()).isEqualTo(MESSAGE_ID);
    }

    private static String messageBody() {
        return "Body";
    }

    private static HttpExecuteResponse responseWithMd5() {
        return HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(200).build()).responseBody(
                                      AbortableInputStream.create(new StringInputStream(
                                          "{\"MD5OfMessageAttributes\":\"43eeb333d10515533e317490584ea243\","
                                          + "\"MD5OfMessageBody\":\"ac101b32dda4448cf13a93fe283dddd8\","
                                          + "\"MessageId\":\"" + MESSAGE_ID + "\"} ")))
                                  .build();
    }

    private static HttpExecuteResponse responseWithoutMd5() {
        return HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream
                                                    .create(new StringInputStream("{\"MessageId\":\"" + MESSAGE_ID + "\"} ")))
                                  .build();
    }

    protected static Map<String, MessageAttributeValue> createAttributeValues() {
        Map<String, MessageAttributeValue> attrs = new HashMap<>();
        attrs.put("attribute-1", MessageAttributeValue.builder().dataType("String").stringValue("tmp").build());
        return Collections.unmodifiableMap(attrs);
    }
}
