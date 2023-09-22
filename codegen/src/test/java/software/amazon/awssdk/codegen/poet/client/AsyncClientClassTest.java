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
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class AsyncClientClassTest {
    @Test
    public void asyncClientClassRestJson() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(ClientTestModels.restJsonServiceModels());
        assertThat(asyncClientClass, generatesTo("test-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassQuery() {
        AsyncClientClass syncClientClass = createAsyncClientClass(ClientTestModels.queryServiceModels());
        assertThat(syncClientClass, generatesTo("test-query-async-client-class.java"));
    }

    @Test
    public void asyncClientClassAwsJson() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(ClientTestModels.awsJsonServiceModels());
        assertThat(asyncClientClass, generatesTo("test-aws-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassAwsQueryCompatibleJson() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(ClientTestModels.awsQueryCompatibleJsonServiceModels());
        assertThat(asyncClientClass, generatesTo("test-aws-query-compatible-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassXml() {
        AsyncClientClass syncClientClass = createAsyncClientClass(ClientTestModels.xmlServiceModels());
        assertThat(syncClientClass, generatesTo("test-xml-async-client-class.java"));
    }

    @Test
    public void asyncClientEndpointDiscovery() {
        AsyncClientClass asyncClientEndpointDiscovery = createAsyncClientClass(ClientTestModels.endpointDiscoveryModels());
        assertThat(asyncClientEndpointDiscovery, generatesTo("test-endpoint-discovery-async.java"));
    }

    @Test
    public void asyncClientCustomServiceMetaData() {
        AsyncClientClass asyncClientCustomServiceMetaData = createAsyncClientClass(ClientTestModels.customContentTypeModels());
        assertThat(asyncClientCustomServiceMetaData, generatesTo("test-customservicemetadata-async.java"));
    }

    private AsyncClientClass createAsyncClientClass(IntermediateModel model) {
        return new AsyncClientClass(GeneratorTaskParams.create(model, "sources/", "tests/", "resources/"));
    }
}
