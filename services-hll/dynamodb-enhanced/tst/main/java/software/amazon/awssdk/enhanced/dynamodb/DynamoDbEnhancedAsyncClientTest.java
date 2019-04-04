/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionCondition;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ResponseItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

public class DynamoDbEnhancedAsyncClientTest {
    private static final DynamoDbAsyncClient GENERATED_CLIENT = Mockito.mock(DynamoDbAsyncClient.class);
    private static final DynamoDbEnhancedAsyncClient CLIENT = DynamoDbEnhancedAsyncClient.builder()
                                                                                         .dynamoDbClient(GENERATED_CLIENT)
                                                                                         .build();

    @AfterClass
    public static void reset() {
        Mockito.reset(GENERATED_CLIENT);
    }

    @Test
    public void tableNameIsCorrect() {
        AsyncTable table = CLIENT.table("table");
        assertThat(table.name()).isEqualTo("table");
    }

    @Test
    public void putItemSendsCorrectValues() {
        ArgumentCaptor<Consumer> putRequestCaptor = ArgumentCaptor.forClass(Consumer.class);
        Mockito.when(GENERATED_CLIENT.putItem(putRequestCaptor.capture()))
               .thenReturn(CompletableFuture.completedFuture(PutItemResponse.builder().build()));

        AsyncTable table = CLIENT.table("table");
        table.putItem(r -> r.putAttribute("foo", "bar")).join();

        PutItemRequest.Builder sentRequestBuilder = PutItemRequest.builder();
        putRequestCaptor.getValue().accept(sentRequestBuilder);
        PutItemRequest sentRequest = sentRequestBuilder.build();

        assertThat(sentRequest.tableName()).isEqualTo("table");
        assertThat(sentRequest.item().get("foo").s()).isEqualTo("bar");
    }

    @Test
    public void getItemSendsCorrectValues() {
        Map<String, AttributeValue> generatedResponseItem = new HashMap<>();
        generatedResponseItem.put("foo2", AttributeValue.builder().s("bar2").build());

        ArgumentCaptor<Consumer> getRequestCaptor = ArgumentCaptor.forClass(Consumer.class);
        GetItemResponse generatedResponse = GetItemResponse.builder().item(generatedResponseItem).build();

        Mockito.when(GENERATED_CLIENT.getItem(getRequestCaptor.capture()))
               .thenReturn(CompletableFuture.completedFuture(generatedResponse));

        AsyncTable table = CLIENT.table("table");
        ResponseItem responseItem = table.getItem(r -> r.putAttribute("foo", "bar")).join();

        GetItemRequest.Builder sentRequestBuilder = GetItemRequest.builder();
        getRequestCaptor.getValue().accept(sentRequestBuilder);
        GetItemRequest sentRequest = sentRequestBuilder.build();

        assertThat(sentRequest.tableName()).isEqualTo("table");
        assertThat(sentRequest.key().get("foo").s()).isEqualTo("bar");
        assertThat(responseItem.attribute("foo2").asString()).isEqualTo("bar2");
    }

    @Test
    public void converterPriorityIsCorrect() {
        Mockito.when(GENERATED_CLIENT.putItem(any(Consumer.class)))
               .thenReturn(CompletableFuture.completedFuture(PutItemResponse.builder().build()));

        ItemAttributeValueConverter clientLevelStringConverter = converter(ConversionCondition.isExactInstanceOf(String.class));
        ItemAttributeValueConverter clientLevelIntegerConverter = converter(ConversionCondition.isExactInstanceOf(Integer.class));

        ItemAttributeValueConverter itemLevelStringConverter = converter(ConversionCondition.isExactInstanceOf(String.class));


        Mockito.when(clientLevelIntegerConverter.toAttributeValue(any(), any())).thenReturn(ItemAttributeValue.fromNumber("1"));
        Mockito.when(itemLevelStringConverter.toAttributeValue(any(), any())).thenReturn(ItemAttributeValue.fromString("bar"));

        DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.builder()
                                                                        .dynamoDbClient(GENERATED_CLIENT)
                                                                        .addConverter(clientLevelStringConverter)
                                                                        .addConverter(clientLevelIntegerConverter)
                                                                        .build();

        AsyncTable table = client.table("table");
        table.putItem(r -> r.putAttribute("foo", "bar")
                            .putAttribute("foo2", 1)
                            .addConverter(itemLevelStringConverter))
             .join();

        Mockito.verify(clientLevelIntegerConverter).toAttributeValue(any(), any());

        Mockito.verify(itemLevelStringConverter).toAttributeValue(any(), any());
        Mockito.verify(clientLevelStringConverter, never()).toAttributeValue(any(), any());
    }

    @Test
    public void closeDoesNotCloseUnderlyingClientWhenProvidedByCustomer() {
        DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(GENERATED_CLIENT).build().close();
        Mockito.verify(GENERATED_CLIENT, never()).close();
    }

    private ItemAttributeValueConverter converter(ConversionCondition condition) {
        ItemAttributeValueConverter converter = Mockito.mock(ItemAttributeValueConverter.class);
        Mockito.when(converter.defaultConversionCondition()).thenReturn(condition);
        return converter;
    }
}