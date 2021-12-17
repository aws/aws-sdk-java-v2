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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecursiveRecordBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecursiveRecordImmutable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ImmutableTableSchemaRecursiveTest {

    @Test
    public void recursiveRecord_document() {
        TableSchema<RecursiveRecordImmutable> tableSchema = TableSchema.fromClass(RecursiveRecordImmutable.class);

        RecursiveRecordBean recursiveRecordBean2 = new RecursiveRecordBean();
        recursiveRecordBean2.setAttribute(4);

        RecursiveRecordBean recursiveRecordBean1 = new RecursiveRecordBean();
        recursiveRecordBean1.setAttribute(3);
        recursiveRecordBean1.setRecursiveRecordBean(recursiveRecordBean2);

        RecursiveRecordImmutable recursiveRecordImmutable2 =
            RecursiveRecordImmutable.builder()
                                    .setAttribute(2)
                                    .setRecursiveRecordBean(recursiveRecordBean1)
                                    .build();

        RecursiveRecordImmutable recursiveRecordImmutable1 =
            RecursiveRecordImmutable.builder()
                                    .setAttribute(1)
                                    .setRecursiveRecordImmutable(recursiveRecordImmutable2)
                                    .build();

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(recursiveRecordImmutable1, true);

        assertThat(itemMap).hasSize(2);
        assertThat(itemMap).containsEntry("attribute", AttributeValue.builder().n("1").build());
        assertThat(itemMap).hasEntrySatisfying("recursiveRecordImmutable", av -> {
            assertThat(av.hasM()).isTrue();
            assertThat(av.m()).containsEntry("attribute", AttributeValue.builder().n("2").build());
            assertThat(av.m()).hasEntrySatisfying("recursiveRecordBean", bav -> {
                assertThat(bav.hasM()).isTrue();
                assertThat(bav.m()).containsEntry("attribute", AttributeValue.builder().n("3").build());
                assertThat(bav.m()).hasEntrySatisfying("recursiveRecordBean", bav2 -> {
                    assertThat(bav2.hasM()).isTrue();
                    assertThat(bav2.m()).containsEntry("attribute", AttributeValue.builder().n("4").build());
                });
            });
        });
    }

    @Test
    public void recursiveRecord_list() {
        TableSchema<RecursiveRecordImmutable> tableSchema =
            TableSchema.fromClass(RecursiveRecordImmutable.class);

        RecursiveRecordImmutable recursiveRecordImmutable2 = RecursiveRecordImmutable.builder()
                                                                                     .setAttribute(2)
                                                                                     .build();

        RecursiveRecordImmutable recursiveRecordImmutable1 =
            RecursiveRecordImmutable.builder()
                                    .setAttribute(1)
                                    .setRecursiveRecordList(Collections.singletonList(recursiveRecordImmutable2))
                                    .build();

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(recursiveRecordImmutable1, true);

        assertThat(itemMap).hasSize(2);
        assertThat(itemMap).containsEntry("attribute", AttributeValue.builder().n("1").build());
        assertThat(itemMap).hasEntrySatisfying("recursiveRecordList", av -> {
            assertThat(av.hasL()).isTrue();
            assertThat(av.l()).hasOnlyOneElementSatisfying(listAv -> {
                assertThat(listAv.hasM()).isTrue();
                assertThat(listAv.m()).containsEntry("attribute", AttributeValue.builder().n("2").build());
            });
        });
    }
}
