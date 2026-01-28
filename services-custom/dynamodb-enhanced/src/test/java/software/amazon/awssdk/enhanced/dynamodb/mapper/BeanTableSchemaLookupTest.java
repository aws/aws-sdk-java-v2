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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class BeanTableSchemaLookupTest extends BaseLookupTest {
    @BeforeEach
    public void cleanup() {
        BeanTableSchema.clearSchemaCache();
    }

    @Test
    void itemMapRoundTrip_cat_pojosLoadedByDifferentClassLoader_providedLookupHasVisibility_works() throws Exception {
        BeanTableSchema catSchema =
            BeanTableSchema.create(BeanTableSchemaParams.builder(getCatClass()).lookup(getPojosLookup()).build());

        Object oscar = makeCat("1", "Oscar");

        Map<String, AttributeValue> attributeValues = catSchema.itemToMap(oscar, true);

        Object roundTrip = catSchema.mapToItem(attributeValues);

        assertThat(roundTrip).isEqualTo(oscar);
    }

    @Test
    void itemMapRoundTrip_recursiveRecordBean_pojosLoadedByDifferentClassLoader_providedLookupHasVisibility_works() throws Exception {
        BeanTableSchema recursiveSchema =
            BeanTableSchema.create(BeanTableSchemaParams.builder(getRecursiveRecordBeanClass())
                                                        .lookup(getPojosLookup())
                                                        .build());

        int attr = 1;
        Object recursive = makeRecursiveRecord(attr, null, null, Collections.emptyList());
        List<Object> recursiveList = new ArrayList<>();
        for (int i = 0; i < 8; ++i) {
            recursiveList.add(makeRecursiveRecord(++attr, null, null, Collections.emptyList()));
        }
        Object recursiveImmutable = makeRecursiveRecordImmutable(++attr, null, null, Collections.emptyList());

        Object bean = makeRecursiveRecord(++attr, recursive, recursiveImmutable, recursiveList);
        Map<String, AttributeValue> map = recursiveSchema.itemToMap(bean, false);

        Object roundTrip = recursiveSchema.mapToItem(map);

        assertThat(roundTrip).isEqualTo(bean);
    }

    @Test
    void itemToMap_pojoLoadedByDifferentClassLoader_providedLookupDoesNotHaveVisibility_fails() throws Exception {
        BeanTableSchema catSchema =
            BeanTableSchema.create(BeanTableSchemaParams.builder(getCatClass()).lookup(MethodHandles.lookup()).build());

        Object oscar = makeCat("1", "Oscar");

        assertThatThrownBy(() -> catSchema.itemToMap(oscar, true)).isInstanceOf(NoClassDefFoundError.class);
    }
}
