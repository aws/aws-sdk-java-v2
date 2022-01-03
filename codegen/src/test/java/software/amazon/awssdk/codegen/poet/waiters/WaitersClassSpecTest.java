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

package software.amazon.awssdk.codegen.poet.waiters;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class WaitersClassSpecTest {

    @Test
    public void asyncWaiterInterface() throws Exception {
        ClassSpec asyncWaiterInterfaceSpec = new AsyncWaiterInterfaceSpec(ClientTestModels.queryServiceModels());
        assertThat(asyncWaiterInterfaceSpec, generatesTo("query-async-waiter-interface.java"));
    }

    @Test
    public void syncWaiterInterface() throws Exception {
        ClassSpec waiterInterface = new WaiterInterfaceSpec(ClientTestModels.queryServiceModels());
        assertThat(waiterInterface, generatesTo("query-sync-waiter-interface.java"));
    }

    @Test
    public void asyncWaiterImpl() throws Exception {
        ClassSpec asyncWaiterInterfaceSpec = new AsyncWaiterClassSpec(ClientTestModels.queryServiceModels());
        assertThat(asyncWaiterInterfaceSpec, generatesTo("query-async-waiter-class.java"));
    }

    @Test
    public void syncWaiterImpl() throws Exception {
        ClassSpec waiterInterface = new WaiterClassSpec(ClientTestModels.queryServiceModels());
        assertThat(waiterInterface, generatesTo("query-sync-waiter-class.java"));
    }
}
