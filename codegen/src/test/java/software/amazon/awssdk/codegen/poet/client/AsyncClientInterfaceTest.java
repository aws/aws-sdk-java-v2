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
import static software.amazon.awssdk.codegen.poet.ClientTestModels.batchManagerModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class AsyncClientInterfaceTest {
    @Test
    public void asyncClientInterface() {
        ClassSpec asyncClientInterface = new AsyncClientInterface(restJsonServiceModels());
        assertThat(asyncClientInterface, generatesTo("test-json-async-client-interface.java"));
    }

    @Test
    public void asyncClientInterfaceWithBatchManager() {
        ClassSpec asyncClientInterface = new AsyncClientInterface(batchManagerModels());
        assertThat(asyncClientInterface, generatesTo("test-json-async-client-interface-batchmanager.java"));
    }
}
