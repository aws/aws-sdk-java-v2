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

package software.amazon.awssdk.benchmark.dynamodb.mock;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Builds deterministic DynamoDB AWS JSON protocol response bodies for mocked Tier C benchmarks.
 */
public final class DynamoDbMockResponseFactory {

    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    private static final URI ENDPOINT = URI.create("https://dynamodb.us-east-1.amazonaws.com");

    private static final OperationInfo OPERATION_INFO = OperationInfo.builder()
                                                                     .httpMethod(SdkHttpMethod.POST)
                                                                     .hasImplicitPayloadMembers(true)
                                                                     .build();

    private static final AwsJsonProtocolMetadata PROTOCOL_METADATA =
        AwsJsonProtocolMetadata.builder()
                               .protocol(AwsJsonProtocol.AWS_JSON)
                               .contentType(CONTENT_TYPE)
                               .build();

    private DynamoDbMockResponseFactory() {
    }

    public static String getItemResponseBody(Map<String, AttributeValue> item) {
        return marshall(GetItemResponse.builder().item(item).build());
    }

    public static String putItemResponseBody() {
        return marshall(PutItemResponse.builder().build());
    }

    /**
     * First-page Query response containing a single deterministic item.
     */
    public static String queryResponseBody(Map<String, AttributeValue> item) {
        return marshall(QueryResponse.builder()
                                     .items(Collections.singletonList(item))
                                     .count(1)
                                     .scannedCount(1)
                                     .build());
    }

    public static byte[] getItemResponseBytes(Map<String, AttributeValue> item) {
        return getItemResponseBody(item).getBytes(StandardCharsets.UTF_8);
    }

    private static String marshall(SdkPojo response) {
        try {
            ProtocolMarshaller<SdkHttpFullRequest> marshaller =
                JsonProtocolMarshallerBuilder.create()
                                             .endpoint(ENDPOINT)
                                             .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY
                                                                .createWriter(CONTENT_TYPE))
                                             .contentType(CONTENT_TYPE)
                                             .operationInfo(OPERATION_INFO)
                                             .sendExplicitNullForPayload(false)
                                             .protocolMetadata(PROTOCOL_METADATA)
                                             .build();

            SdkHttpFullRequest request = marshaller.marshall(response);
            return request.contentStreamProvider()
                          .map(provider -> FunctionalUtils.invokeSafely(
                              () -> IoUtils.toUtf8String(provider.newStream())))
                          .orElse("{}");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to marshall DynamoDB mock response", e);
        }
    }
}
