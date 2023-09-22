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
import static software.amazon.awssdk.codegen.poet.ClientTestModels.awsQueryCompatibleJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.customContentTypeModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.endpointDiscoveryModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.xmlServiceModels;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class SyncClientClassTest {
    @Test
    public void syncClientClassRestJson() {
        SyncClientClass syncClientClass = createSyncClientClass(restJsonServiceModels());
        assertThat(syncClientClass, generatesTo("test-json-client-class.java"));
    }

    @Test
    public void syncClientClassQuery() {
        SyncClientClass syncClientClass = createSyncClientClass(queryServiceModels());
        assertThat(syncClientClass, generatesTo("test-query-client-class.java"));
    }

    @Test
    public void syncClientClassAwsQueryCompatibleJson() {
        SyncClientClass syncClientClass = createSyncClientClass(awsQueryCompatibleJsonServiceModels());
        assertThat(syncClientClass, generatesTo("test-aws-query-compatible-json-sync-client-class.java"));
    }

    @Test
    public void syncClientClassXml() {
        SyncClientClass syncClientClass = createSyncClientClass(xmlServiceModels());
        assertThat(syncClientClass, generatesTo("test-xml-client-class.java"));
    }

    @Test
    public void syncClientEndpointDiscovery() {
        ClassSpec syncClientEndpointDiscovery = createSyncClientClass(endpointDiscoveryModels());
        assertThat(syncClientEndpointDiscovery, generatesTo("test-endpoint-discovery-sync.java"));
    }

    @Test
    public void syncClientCustomServiceMetaData() {
        ClassSpec syncClientCustomServiceMetaData = createSyncClientClass(customContentTypeModels());
        assertThat(syncClientCustomServiceMetaData, generatesTo("test-customservicemetadata-sync.java"));
    }

    private SyncClientClass createSyncClientClass(IntermediateModel model) {
        return new SyncClientClass(GeneratorTaskParams.create(model, "sources/", "tests/", "resources/"));
    }
}
