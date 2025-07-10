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

package software.amazon.awssdk.codegen.poet.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.presignedUrlModels;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class SyncClientInterfaceTest {
    @Test
    public void syncClientInterface() {
        ClassSpec syncClientInterface = new SyncClientInterface(restJsonServiceModels());
        assertThat(syncClientInterface, generatesTo("test-json-client-interface.java"));
    }

    @Test
    public void syncClientInterfaceWithPresignedUrlManager() {
        ClassSpec syncClientInterface = new SyncClientInterface(presignedUrlModels());
        assertThat(syncClientInterface, generatesTo("test-json-sync-client-interface-presignedurl.java"));
    }
}
