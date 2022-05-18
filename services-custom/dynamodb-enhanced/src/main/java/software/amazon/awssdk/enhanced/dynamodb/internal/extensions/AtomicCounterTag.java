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

package software.amazon.awssdk.enhanced.dynamodb.internal.extensions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.AtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;

@SdkInternalApi
public class AtomicCounterTag implements StaticAttributeTag {

    public static final String CUSTOM_METADATA_KEY_PREFIX = "AtomicCounter:Counters";

    private static final long DEFAULT_INCREMENT = 1L;
    private static final long DEFAULT_START_VALUE = 0L;

    private static final AtomicCounter DEFAULT_COUNTER = AtomicCounter.builder()
                                                                      .delta(DEFAULT_INCREMENT)
                                                                      .startValue(DEFAULT_START_VALUE)
                                                                      .build();

    private final AtomicCounter counter;

    private AtomicCounterTag(AtomicCounter counter) {
        this.counter = counter;
    }

    public static AtomicCounterTag create() {
        return new AtomicCounterTag(DEFAULT_COUNTER);
    }

    public static AtomicCounterTag fromValues(long delta, long startValue) {
        return new AtomicCounterTag(AtomicCounter.builder()
                                                 .delta(delta)
                                                 .startValue(startValue)
                                                 .build());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, AtomicCounter> resolve(TableMetadata tableMetadata) {
        return tableMetadata.customMetadataObject(CUSTOM_METADATA_KEY_PREFIX, Map.class).orElseGet(HashMap::new);
    }

    @Override
    public Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName, AttributeValueType attributeValueType) {
        return metadata -> metadata
            .addCustomMetadataObject(CUSTOM_METADATA_KEY_PREFIX, Collections.singletonMap(attributeName, this.counter))
            .markAttributeAsKey(attributeName, attributeValueType);
    }
}
