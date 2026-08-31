/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior;
import software.amazon.awssdk.mapper.dynamodb.pojos.StringAttributeClass;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

public class TrustedRequestMapTest {
    @Test
    public void save_transformerRetainsReturnedMap_requestDoesNotAliasTransformerMap() {
        DynamoDbClient dynamoDb = mock(DynamoDbClient.class);
        when(dynamoDb.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        RetainingTransformer transformer = new RetainingTransformer();
        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                                                           .withSaveBehavior(SaveBehavior.CLOBBER)
                                                           .build();
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDb, config, transformer);

        StringAttributeClass item = new StringAttributeClass();
        item.setKey("key");
        item.setStringAttribute("value");
        mapper.save(item);

        ArgumentCaptor<PutItemRequest> request = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDb).putItem(request.capture());

        transformer.returned.put("mutated", AttributeValue.builder().s("later").build());
        assertFalse(request.getValue().item().containsKey("mutated"));
    }

    private static final class RetainingTransformer implements AttributeTransformer {
        private Map<String, AttributeValue> returned;

        @Override
        public Map<String, AttributeValue> transform(Parameters<?> parameters) {
            returned = new HashMap<>(parameters.getAttributeValues());
            return returned;
        }

        @Override
        public Map<String, AttributeValue> untransform(Parameters<?> parameters) {
            return parameters.getAttributeValues();
        }
    }
}
