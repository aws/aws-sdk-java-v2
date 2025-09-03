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

package software.amazon.awssdk.utilslite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utilslite.internal.SdkInternalThreadLocal;

import static org.assertj.core.api.Assertions.assertThat;

class SdkInternalThreadLocalTest {

    @AfterEach
    void cleanup() {
        SdkInternalThreadLocal.clear();
    }

    @Test
    void putAndGet_shouldStoreAndRetrieveValue() {
        SdkInternalThreadLocal.put("test-key", "test-value");
        
        assertThat(SdkInternalThreadLocal.get("test-key")).isEqualTo("test-value");
    }

    @Test
    void get_withNonExistentKey_shouldReturnNull() {
        assertThat(SdkInternalThreadLocal.get("non-existent")).isNull();
    }

    @Test
    void put_withValidKeyValue_shouldStoreValue() {
        SdkInternalThreadLocal.put("test-key", "test-value");
        
        String removed = SdkInternalThreadLocal.remove("test-key");
        
        assertThat(removed).isEqualTo("test-value");
        assertThat(SdkInternalThreadLocal.get("test-key")).isNull();
    }

    @Test
    void remove_withExistingKey_shouldRemoveAndReturnValue() {
        SdkInternalThreadLocal.put("test-key", "test-value");
        SdkInternalThreadLocal.put("test-key", null);
        
        assertThat(SdkInternalThreadLocal.get("test-key")).isNull();
    }

    @Test
    void clear_withMultipleValues_shouldRemoveAllValues() {
        SdkInternalThreadLocal.put("key1", "value1");
        SdkInternalThreadLocal.put("key2", "value2");
        
        SdkInternalThreadLocal.clear();
        
        assertThat(SdkInternalThreadLocal.get("key1")).isNull();
        assertThat(SdkInternalThreadLocal.get("key2")).isNull();
    }
}