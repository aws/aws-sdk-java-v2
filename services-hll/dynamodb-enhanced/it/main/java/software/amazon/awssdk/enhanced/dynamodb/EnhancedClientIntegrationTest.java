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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.RequestItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ResponseItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.testutils.Waiter;

public class EnhancedClientIntegrationTest {
    private static final String TABLE = "books-" + UUID.randomUUID();
    private static final DynamoDbClient dynamo = DynamoDbClient.create();

    @BeforeClass
    public static void setup() {
        try {
            dynamo.createTable(r -> r.tableName(TABLE)
                                     .keySchema(k -> k.attributeName("isbn").keyType(KeyType.HASH))
                                     .attributeDefinitions(a -> a.attributeName("isbn").attributeType(ScalarAttributeType.S))
                                     .provisionedThroughput(t -> t.readCapacityUnits(5L)
                                                                  .writeCapacityUnits(5L)));
        } catch (ResourceInUseException e) {
            // Table already exists. Awesome.
        }

        System.out.println("Waiting for table to be active...");

        Waiter.run(() -> dynamo.describeTable(r -> r.tableName(TABLE)))
              .until(r -> r.table().tableStatus().equals(TableStatus.ACTIVE))
              .orFail();
    }

    @AfterClass
    public static void cleanup() {
        boolean deleted =
                Waiter.run(() -> dynamo.deleteTable(r -> r.tableName(TABLE)))
                      .ignoringException(DynamoDbException.class)
                      .orReturnFalse();

        if (!deleted) {
            System.out.println("Table could not be cleaned up.");
        }
    }

    @Test
    public void getCanReadTheResultOfPut() throws InterruptedException {
        try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create()) {
            Table books = client.table("books");

            System.out.println("Putting item...");

            books.putItem(requestItem());

            Thread.sleep(5_000);

            System.out.println("Getting item...");

            ResponseItem book = books.getItem(requestItemKey());

            validateResponseItem(book);
        }
    }

    @Test
    public void getCanReadTheResultOfPutAsync() throws InterruptedException {
        try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.create()) {
            AsyncTable books = client.table("books");

            System.out.println("Putting item...");

            books.putItem(requestItem()).join();

            Thread.sleep(5_000);

            System.out.println("Getting item...");

            ResponseItem book = books.getItem(requestItemKey()).join();

            validateResponseItem(book);
        }
    }

    private RequestItem requestItem() {
        return RequestItem.builder()
                          .putAttribute("isbn", "0-330-25864-8")
                          .putAttribute("title", "The Hitchhiker's Guide to the Galaxy")
                          .putAttribute("publicationDate", p -> p.putAttribute("UK", Instant.parse("1979-10-12T00:00:00Z"))
                                                                 .putAttribute("US", Instant.parse("1980-01-01T00:00:00Z")))
                          .putAttribute("authors", Collections.singletonList("Douglas Adams"))
                          .build();
    }

    private RequestItem requestItemKey() {
        return RequestItem.builder()
                          .putAttribute("isbn", "0-330-25864-8")
                          .build();
    }

    private void validateResponseItem(ResponseItem book) {
        Map<String, Instant> publicationDates = new LinkedHashMap<>();
        publicationDates.put("UK", Instant.parse("1979-10-12T00:00:00Z"));
        publicationDates.put("US", Instant.parse("1980-01-01T00:00:00Z"));

        assertThat(book.attribute("isbn").asString()).isEqualTo("0-330-25864-8");
        assertThat(book.attribute("title").asString()).isEqualTo("The Hitchhiker's Guide to the Galaxy");
        assertThat(book.attribute("publicationDate").asMap(String.class, Instant.class)).isEqualTo(publicationDates);
        assertThat(book.attribute("authors").asList(String.class)).isEqualTo(Collections.singletonList("Douglas Adams"));
    }
}
