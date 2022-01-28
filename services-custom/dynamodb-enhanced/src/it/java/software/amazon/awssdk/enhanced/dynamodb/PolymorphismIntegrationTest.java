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

package software.amazon.awssdk.enhanced.dynamodb;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.StringJoiner;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypeName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes.Subtype;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class PolymorphismIntegrationTest {
    private static DynamoDbEnhancedClient client;
    private static DynamoDbTable<TableEntry> orderingTable;

    @BeforeClass
    public static void setup() {
        DynamoDbClient llc = DynamoDbClient.create();
        client = DynamoDbEnhancedClient.builder().dynamoDbClient(llc).build();
        orderingTable = client.table("ordering", TableSchema.fromClass(TableEntry.class));
        orderingTable.createTable(r -> r.globalSecondaryIndices(EnhancedGlobalSecondaryIndex.builder()
                                                                                            .indexName("gsi1")
                                                                                            .projection(p -> p.projectionType(ProjectionType.ALL))
                                                                                            .build(),
                                                                EnhancedGlobalSecondaryIndex.builder()
                                                                                            .indexName("gsi2")
                                                                                            .projection(p -> p.projectionType(ProjectionType.ALL))
                                                                                            .build()));
        llc.waiter().waitUntilTableExists(r -> r.tableName("ordering"));

        User user = new User();
        user.setUserId(1L);
        user.setEmail("millem@amazon.com");
        orderingTable.putItem(user);

        Order order1 = new Order();
        order1.setUserId(1L);
        order1.setOrderId(1L);
        order1.setCreationDate(LocalDate.of(2022, 1, 26));

        OrderLineItem order1LineItem1 = new OrderLineItem();
        order1LineItem1.setUserId(order1.getUserId());
        order1LineItem1.setOrderId(order1.getOrderId());
        order1LineItem1.setOrderCreationDate(order1.getCreationDate());
        order1LineItem1.setProductId(1L);
        order1LineItem1.setPurchaseName("cheese");
        order1LineItem1.setPurchasePrice(new BigDecimal("1.30"));

        OrderLineItem order1LineItem2 = new OrderLineItem();
        order1LineItem2.setUserId(order1.getUserId());
        order1LineItem2.setOrderId(order1.getOrderId());
        order1LineItem2.setOrderCreationDate(order1.getCreationDate());
        order1LineItem2.setProductId(2L);
        order1LineItem2.setPurchaseName("sauce");
        order1LineItem2.setPurchasePrice(new BigDecimal("1.90"));

        client.transactWriteItems(r -> r.addPutItem(orderingTable, order1)
                                        .addPutItem(orderingTable, order1LineItem1)
                                        .addPutItem(orderingTable, order1LineItem2));


        Order order2 = new Order();
        order2.setUserId(1L);
        order2.setOrderId(2L);
        order2.setCreationDate(LocalDate.of(2022, 1, 28));

        OrderLineItem order2LineItem1 = new OrderLineItem();
        order2LineItem1.setUserId(order2.getUserId());
        order2LineItem1.setOrderId(order2.getOrderId());
        order2LineItem1.setOrderCreationDate(order2.getCreationDate());
        order2LineItem1.setProductId(3L);
        order2LineItem1.setPurchaseName("dough");
        order2LineItem1.setPurchasePrice(new BigDecimal("3.99"));

        client.transactWriteItems(r -> r.addPutItem(orderingTable, order2)
                                        .addPutItem(orderingTable, order2LineItem1));
    }

    @AfterClass
    public static void teardown() {
        orderingTable.deleteTable();
    }

    @Test
    public void getAllOrdersByUser() {
        orderingTable.query(QueryConditional.sortBeginsWith(r -> r.partitionValue("user#1")
                                                                  .sortValue("order#")))
                     .items()
                     .forEach(System.out::println);
    }

    @Test
    public void getProductsByOrder() {
        orderingTable.index("gsi1")
                     .query(QueryConditional.sortBeginsWith(r -> r.partitionValue("order#1")
                                                                  .sortValue("product#")))
                     .stream()
                     .flatMap(p -> p.items().stream())
                     .forEach(System.out::println);
    }

    @Test
    public void getOrderLineItemsByProduct() {
        orderingTable.index("gsi2")
                     .query(QueryConditional.sortBeginsWith(r -> r.partitionValue("product#1")
                                                                  .sortValue("order#")))
                     .stream()
                     .flatMap(p -> p.items().stream())
                     .forEach(System.out::println);
    }

    @Test
    public void getAllOrdersBetweenTwoDates() {
        orderingTable.query(QueryConditional.sortBetween(r -> r.partitionValue("user#1")
                                                               .sortValue("order#2022-01-26#"),
                                                         r -> r.partitionValue("user#1")
                                                               .sortValue("order#2022-01-27#")))
                     .items()
                     .forEach(System.out::println);
    }

    @DynamoDbBean
    @DynamoDbSubtypes({ @Subtype(name = "user", subtypeClass = User.class),
                        @Subtype(name = "order", subtypeClass = Order.class),
                        @Subtype(name = "order-line-item", subtypeClass = OrderLineItem.class) })
    public abstract static class TableEntry {
        // @DynamoDbAttribute("type")
        @DynamoDbSubtypeName
        public abstract String getType();
        public void setType(String type) {}

        @DynamoDbAttribute("PK")
        @DynamoDbPartitionKey
        public abstract String getPartitionKey();
        public abstract void setPartitionKey(String partitionKey);

        @DynamoDbAttribute("SK")
        @DynamoDbSortKey
        public abstract String getSortKey();
        public abstract void setSortKey(String sortKey);

        @DynamoDbAttribute("GSI1PK")
        @DynamoDbSecondaryPartitionKey(indexNames = "gsi1")
        public String getGsi1PrimaryKey() { return null; }
        public void setGsi1PrimaryKey(String gsi1PrimaryKey) {}

        @DynamoDbAttribute("GSI1SK")
        @DynamoDbSecondarySortKey(indexNames = "gsi1")
        public String getGsi1SortKey() { return null; }
        public void setGsi1SortKey(String gsi1SortKey) {}

        @DynamoDbAttribute("GSI2PK")
        @DynamoDbSecondaryPartitionKey(indexNames = "gsi2")
        public String getGsi2PrimaryKey() { return null; }
        public void setGsi2PrimaryKey(String gsi1PrimaryKey) {}

        @DynamoDbAttribute("GSI2SK")
        @DynamoDbSecondarySortKey(indexNames = "gsi2")
        public String getGsi2SortKey() { return null; }
        public void setGsi2SortKey(String gsi1SortKey) {}
    }

    @DynamoDbBean
    public static class User extends TableEntry {
        private Long userId;
        private String email;

        @Override
        public String getType() {
            return "user";
        }

        @DynamoDbAttribute("PK")
        @DynamoDbPartitionKey
        @Override
        public String getPartitionKey() {
            return "user#" + userId;
        }

        @Override
        public void setPartitionKey(String partitionKey) {
            String[] splitKey = StringUtils.split(partitionKey, '#');
            setUserId(Long.parseLong(splitKey[1]));
        }

        @DynamoDbAttribute("SK")
        @DynamoDbSortKey
        @Override
        public String getSortKey() {
            return "#";
        }

        @Override
        public void setSortKey(String sortKey) {
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", User.class.getSimpleName() + "[", "]")
                .add("userId=" + userId)
                .add("email='" + email + "'")
                .toString();
        }
    }

    @DynamoDbBean
    public static class Order extends TableEntry {
        private Long userId;
        private Long orderId;
        private LocalDate creationDate;
        private Instant shipTime;

        @Override
        public String getType() {
            return "order";
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        @DynamoDbAttribute("PK")
        @DynamoDbPartitionKey
        @Override
        public String getPartitionKey() {
            return "user#" + userId;
        }

        @Override
        public void setPartitionKey(String partitionKey) {
            String[] splitKey = StringUtils.split(partitionKey, '#');
            setUserId(Long.parseLong(splitKey[1]));
        }

        @DynamoDbAttribute("SK")
        @DynamoDbSortKey
        @Override
        public String getSortKey() {
            return "order#" + creationDate + '#' + orderId;
        }

        @Override
        public void setSortKey(String sortKey) {
            String[] splitKey = StringUtils.split(sortKey, '#');
            setCreationDate(LocalDate.parse(splitKey[1]));
            setOrderId(Long.parseLong(splitKey[2]));
        }

        @DynamoDbAttribute("GSI1PK")
        @DynamoDbSecondaryPartitionKey(indexNames = "gsi1")
        @Override
        public String getGsi1PrimaryKey() {
            return "order#" + orderId;
        }

        @DynamoDbAttribute("GSI1SK")
        @DynamoDbSecondarySortKey(indexNames = "gsi1")
        @Override
        public String getGsi1SortKey() {
            return "#";
        }

        public LocalDate getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(LocalDate creationDate) {
            this.creationDate = creationDate;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        @DynamoDbAttribute("shipTime")
        public Instant getShipTime() {
            return shipTime;
        }

        public void setShipTime(Instant shipTime) {
            this.shipTime = shipTime;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Order.class.getSimpleName() + "[", "]")
                .add("userId=" + userId)
                .add("orderId=" + orderId)
                .add("creationDate=" + creationDate)
                .add("shipTime=" + shipTime)
                .toString();
        }
    }

    @DynamoDbBean
    public static class OrderLineItem extends TableEntry {
        private Long userId;
        private Long orderId;
        private Long productId;
        private LocalDate orderCreationDate;
        private String purchaseName;
        private BigDecimal purchasePrice;

        @Override
        public String getType() {
            return "order-line-item";
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        @DynamoDbAttribute("PK")
        @DynamoDbPartitionKey
        @Override
        public String getPartitionKey() {
            return "user#" + userId;
        }

        @Override
        public void setPartitionKey(String partitionKey) {
            String[] splitKey = StringUtils.split(partitionKey, '#');
            setUserId(Long.parseLong(splitKey[1]));
        }

        @DynamoDbAttribute("SK")
        @DynamoDbSortKey
        @Override
        public String getSortKey() {
            return "order#" + orderCreationDate + '#' + orderId + '#' + productId;
        }

        @Override
        public void setSortKey(String sortKey) {
            String[] splitKey = StringUtils.split(sortKey, '#');
            setOrderCreationDate(LocalDate.parse(splitKey[1]));
            setOrderId(Long.parseLong(splitKey[2]));
        }

        @DynamoDbAttribute("GSI1PK")
        @DynamoDbSecondaryPartitionKey(indexNames = "gsi1")
        @Override
        public String getGsi1PrimaryKey() {
            return "order#" + orderId;
        }

        @DynamoDbAttribute("GSI1SK")
        @DynamoDbSecondarySortKey(indexNames = "gsi1")
        @Override
        public String getGsi1SortKey() {
            return "product#" + productId;
        }

        @DynamoDbAttribute("GSI2PK")
        @DynamoDbSecondaryPartitionKey(indexNames = "gsi2")
        @Override
        public String getGsi2PrimaryKey() {
            return "product#" + productId;
        }

        @DynamoDbAttribute("GSI2SK")
        @DynamoDbSecondarySortKey(indexNames = "gsi2")
        @Override
        public String getGsi2SortKey() {
            return "order#" + orderId;
        }

        public LocalDate getOrderCreationDate() {
            return orderCreationDate;
        }

        public void setOrderCreationDate(LocalDate orderCreationDate) {
            this.orderCreationDate = orderCreationDate;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        @DynamoDbAttribute("purchaseName")
        public String getPurchaseName() {
            return purchaseName;
        }

        public void setPurchaseName(String purchaseName) {
            this.purchaseName = purchaseName;
        }

        @DynamoDbAttribute("purchasePrice")
        public BigDecimal getPurchasePrice() {
            return purchasePrice;
        }

        public void setPurchasePrice(BigDecimal purchasePrice) {
            this.purchasePrice = purchasePrice;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", OrderLineItem.class.getSimpleName() + "[", "]")
                .add("userId=" + userId)
                .add("orderId=" + orderId)
                .add("productId=" + productId)
                .add("orderCreationDate=" + orderCreationDate)
                .add("purchaseName='" + purchaseName + "'")
                .add("purchasePrice=" + purchasePrice)
                .toString();
        }
    }
}
