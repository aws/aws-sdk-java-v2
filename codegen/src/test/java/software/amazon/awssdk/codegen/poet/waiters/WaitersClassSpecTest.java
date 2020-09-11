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

import java.io.File;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.poet.client.AsyncClientClass;
import software.amazon.awssdk.codegen.poet.client.AsyncClientInterface;
import software.amazon.awssdk.codegen.poet.client.ClientSimpleMethodsIntegrationTests;
import software.amazon.awssdk.codegen.poet.client.SyncClientClass;
import software.amazon.awssdk.codegen.poet.client.SyncClientInterface;
import software.amazon.awssdk.codegen.poet.paginators.PaginatedResponseClassSpecTest;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

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
