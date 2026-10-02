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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class FakeItemWithCompositeGsi {
    private static final StaticTableSchema<FakeItemWithCompositeGsi> TABLE_SCHEMA =
        StaticTableSchema.builder(FakeItemWithCompositeGsi.class)
                         .newItemSupplier(FakeItemWithCompositeGsi::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(FakeItemWithCompositeGsi::getId)
                                                           .setter(FakeItemWithCompositeGsi::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("sort")
                                                           .getter(FakeItemWithCompositeGsi::getSort)
                                                           .setter(FakeItemWithCompositeGsi::setSort)
                                                           .tags(primarySortKey()))
                         .addAttribute(String.class, a -> a.name("gsi_pk1")
                                                           .getter(FakeItemWithCompositeGsi::getGsiPk1)
                                                           .setter(FakeItemWithCompositeGsi::setGsiPk1)
                                                           .tags(secondaryPartitionKey("composite_gsi", Order.FIRST)))
                         .addAttribute(String.class, a -> a.name("gsi_pk2")
                                                           .getter(FakeItemWithCompositeGsi::getGsiPk2)
                                                           .setter(FakeItemWithCompositeGsi::setGsiPk2)
                                                           .tags(secondaryPartitionKey("composite_gsi", Order.SECOND)))
                         .addAttribute(String.class, a -> a.name("gsi_sk1")
                                                           .getter(FakeItemWithCompositeGsi::getGsiSk1)
                                                           .setter(FakeItemWithCompositeGsi::setGsiSk1)
                                                           .tags(secondarySortKey("composite_gsi", Order.FIRST)))
                         .addAttribute(String.class, a -> a.name("gsi_sk2")
                                                           .getter(FakeItemWithCompositeGsi::getGsiSk2)
                                                           .setter(FakeItemWithCompositeGsi::setGsiSk2)
                                                           .tags(secondarySortKey("composite_gsi", Order.SECOND)))
                         .build();

    private String id;
    private String sort;
    private String gsiPk1;
    private String gsiPk2;
    private String gsiSk1;
    private String gsiSk2;

    public static StaticTableSchema<FakeItemWithCompositeGsi> getTableSchema() {
        return TABLE_SCHEMA;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getGsiPk1() {
        return gsiPk1;
    }

    public void setGsiPk1(String gsiPk1) {
        this.gsiPk1 = gsiPk1;
    }

    public String getGsiPk2() {
        return gsiPk2;
    }

    public void setGsiPk2(String gsiPk2) {
        this.gsiPk2 = gsiPk2;
    }

    public String getGsiSk1() {
        return gsiSk1;
    }

    public void setGsiSk1(String gsiSk1) {
        this.gsiSk1 = gsiSk1;
    }

    public String getGsiSk2() {
        return gsiSk2;
    }

    public void setGsiSk2(String gsiSk2) {
        this.gsiSk2 = gsiSk2;
    }
}