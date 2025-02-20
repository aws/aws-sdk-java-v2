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

public class ImmutableTableSchemaLookupTest extends BaseLookupTest {

    @BeforeEach
    public void cleanup() {
        ImmutableTableSchema.clearSchemaCache();
    }

    @Test
    public void itemMapRoundTrip_catImmutable_loadedByDifferentClassLoader_providedLookupHasVisibility_works() throws Exception {
        ImmutableTableSchema schema =
            ImmutableTableSchema.create(ImmutableTableSchemaParams.builder(getCatImmutableClass()).lookup(getPojosLookup()).build());


        Object catImmutable = makeCatImmutable("1", "Oscar");

        Map<String, AttributeValue> map = schema.itemToMap(catImmutable, false);
        assertThat(schema.mapToItem(map)).isEqualTo(catImmutable);
    }

    @Test
    public void itemMapRoundTrip_recursiveImmutable_loadedByDifferentClassLoader_providedLookupHasVisibility_works() throws Exception {
        ImmutableTableSchema schema =
            ImmutableTableSchema.create(ImmutableTableSchemaParams.builder(getRecursiveRecordImmutableClass()).lookup(getPojosLookup()).build());


        int attr = 0;
        Object recursiveBean = makeRecursiveRecord(++attr, null, null, Collections.emptyList());
        Object recursiveImmutable = makeRecursiveRecordImmutable(++attr, null, null, Collections.emptyList());
        List<Object> listOfRecursiveImmutable = new ArrayList<>();
        for (int i = 0; i < 8; ++i) {
            listOfRecursiveImmutable.add(makeRecursiveRecordImmutable(++attr, null, null, null));
        }

        Object immutable = makeRecursiveRecordImmutable(++attr, recursiveBean, recursiveImmutable, listOfRecursiveImmutable);
        Map<String, AttributeValue> map = schema.itemToMap(immutable, false);
        assertThat(schema.mapToItem(map)).isEqualTo(immutable);
    }

    @Test
    void itemToMap_pojoLoadedByDifferentClassLoader_providedLookupDoesNotHaveVisibility_fails() throws Exception {
        ImmutableTableSchema schema =
            ImmutableTableSchema.create(ImmutableTableSchemaParams.builder(getCatImmutableClass()).lookup(MethodHandles.lookup()).build());


        Object catImmutable = makeCatImmutable("1", "Oscar");

        assertThatThrownBy(() -> schema.itemToMap(catImmutable, false)).isInstanceOf(NoClassDefFoundError.class);
    }
}
