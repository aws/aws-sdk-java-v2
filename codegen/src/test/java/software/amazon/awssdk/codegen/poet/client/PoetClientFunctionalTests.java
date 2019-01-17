/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class PoetClientFunctionalTests {

    @Test
    public void asyncClientClass() throws Exception {
        AsyncClientClass asyncClientClass = new AsyncClientClass(
                GeneratorTaskParams.create(ClientTestModels.jsonServiceModels(), "sources/", "tests/"));
        assertThat(asyncClientClass, generatesTo("test-async-client-class.java"));
    }

    @Test
    public void asyncClientInterface() throws Exception {
        ClassSpec asyncClientInterface = new AsyncClientInterface(ClientTestModels.jsonServiceModels());
        assertThat(asyncClientInterface, generatesTo("test-json-async-client-interface.java"));
    }

    @Test
    public void simpleMethodsIntegClass() throws Exception {
        ClientSimpleMethodsIntegrationTests simpleMethodsClass = new ClientSimpleMethodsIntegrationTests(
                ClientTestModels.jsonServiceModels());
        assertThat(simpleMethodsClass, generatesTo("test-simple-methods-integ-class.java"));
    }

    @Test
    public void syncClientClassJson() throws Exception {
        SyncClientClass syncClientClass = createSyncClientClass(ClientTestModels.jsonServiceModels());
        assertThat(syncClientClass, generatesTo("test-json-client-class.java"));
    }

    @Test
    public void syncClientClassQuery() throws Exception {
        SyncClientClass syncClientClass = createSyncClientClass(ClientTestModels.queryServiceModels());
        assertThat(syncClientClass, generatesTo("test-query-client-class.java"));
    }

    private SyncClientClass createSyncClientClass(IntermediateModel model) {
        return new SyncClientClass(GeneratorTaskParams.create(model, "sources/", "tests/"));
    }

    @Test
    public void syncClientInterface() throws Exception {
        ClassSpec syncClientInterface = new SyncClientInterface(ClientTestModels.jsonServiceModels());
        assertThat(syncClientInterface, generatesTo("test-json-client-interface.java"));
    }

    @Test
    public void syncClientEndpointDiscovery() throws Exception {
        ClassSpec syncClientEndpointDiscovery = createSyncClientClass(ClientTestModels.endpointDiscoveryModels());
        assertThat(syncClientEndpointDiscovery, generatesTo("test-endpoint-discovery-sync.java"));
    }

    @Test
    public void asyncClientEndpointDiscovery() throws Exception {
        ClassSpec asyncClientEndpointDiscovery = new AsyncClientClass(
                GeneratorTaskParams.create(ClientTestModels.endpointDiscoveryModels(), "sources/", "tests/"));
        assertThat(asyncClientEndpointDiscovery, generatesTo("test-endpoint-discovery-async.java"));
    }
}
