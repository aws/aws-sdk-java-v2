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

package software.amazon.awssdk.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadStorageTest {

    @AfterEach
    void cleanup() {
        ThreadStorage.clear();
    }

    @Test
    void put_withValidKeyValue_shouldStoreValue() {
        ThreadStorage.put("test-key", "test-value");
        
        assertThat(ThreadStorage.get("test-key")).isEqualTo("test-value");
    }

    @Test
    void get_withNonExistentKey_shouldReturnNull() {
        assertThat(ThreadStorage.get("non-existent")).isNull();
    }

    @Test
    void remove_withExistingKey_shouldRemoveAndReturnValue() {
        ThreadStorage.put("test-key", "test-value");
        
        String removed = ThreadStorage.remove("test-key");
        
        assertThat(removed).isEqualTo("test-value");
        assertThat(ThreadStorage.get("test-key")).isNull();
    }

    @Test
    void put_withNullValue_shouldRemoveKey() {
        ThreadStorage.put("test-key", "test-value");
        ThreadStorage.put("test-key", null);
        
        assertThat(ThreadStorage.get("test-key")).isNull();
    }

    @Test
    void clear_withMultipleValues_shouldRemoveAllValues() {
        ThreadStorage.put("key1", "value1");
        ThreadStorage.put("key2", "value2");
        
        ThreadStorage.clear();
        
        assertThat(ThreadStorage.get("key1")).isNull();
        assertThat(ThreadStorage.get("key2")).isNull();
    }

    @Test
    void containsKey_withExistingKey_shouldReturnTrue() {
        ThreadStorage.put("test-key", "test-value");
        
        assertThat(ThreadStorage.containsKey("test-key")).isTrue();
    }

    @Test
    void containsKey_withNonExistentKey_shouldReturnFalse() {
        assertThat(ThreadStorage.containsKey("non-existent")).isFalse();
    }

    @Test
    void get_fromDifferentThread_shouldNotShareData() throws InterruptedException {
        ThreadStorage.put("shared-key", "main-thread-value");
        
        Thread otherThread = new Thread(() -> {
            // Should not see main thread's value
            assertThat(ThreadStorage.get("shared-key")).isNull();
            
            // Set own value
            ThreadStorage.put("shared-key", "other-thread-value");
            assertThat(ThreadStorage.get("shared-key")).isEqualTo("other-thread-value");
        });
        
        otherThread.start();
        otherThread.join();
        
        // Main thread should still have its own value
        assertThat(ThreadStorage.get("shared-key")).isEqualTo("main-thread-value");
    }
}