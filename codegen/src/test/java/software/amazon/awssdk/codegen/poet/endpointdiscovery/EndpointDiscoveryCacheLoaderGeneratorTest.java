/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.codegen.poet.endpointdiscovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class EndpointDiscoveryCacheLoaderGeneratorTest {

    @Test
    public void syncEndpointDiscoveryCacheLoaderGenerator() {
        IntermediateModel model = ClientTestModels.endpointDiscoveryModels();
        GeneratorTaskParams dependencies = GeneratorTaskParams.create(model, "sources/", "tests/");
        EndpointDiscoveryCacheLoaderGenerator cacheLoader = new EndpointDiscoveryCacheLoaderGenerator(dependencies);
        assertThat(cacheLoader, generatesTo("test-sync-cache-loader.java"));
    }

    @Test
    public void asyncEndpointDiscoveryCacheLoaderGenerator() {
        IntermediateModel model = ClientTestModels.endpointDiscoveryModels();
        GeneratorTaskParams dependencies = GeneratorTaskParams.create(model, "sources/", "tests/");
        EndpointDiscoveryAsyncCacheLoaderGenerator cacheLoader = new EndpointDiscoveryAsyncCacheLoaderGenerator(dependencies);
        assertThat(cacheLoader, generatesTo("test-async-cache-loader.java"));
    }
}
