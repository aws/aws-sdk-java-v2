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
import static software.amazon.awssdk.codegen.poet.ClientTestModels.awsJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.awsQueryCompatibleJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.batchManagerModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.customContentTypeModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.customPackageModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.endpointDiscoveryModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.rpcv2ServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.xmlServiceModels;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class AsyncClientClassTest {
    @Test
    public void asyncClientClassRestJson() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(restJsonServiceModels(), false);
        assertThat(asyncClientClass, generatesTo("test-json-async-client-class.java"));

        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(restJsonServiceModels(), true);
        assertThat(sraAsyncClientClass, generatesTo("sra/test-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassQuery() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(queryServiceModels(), false);
        assertThat(asyncClientClass, generatesTo("test-query-async-client-class.java"));

        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(queryServiceModels(), true);
        assertThat(sraAsyncClientClass, generatesTo("sra/test-query-async-client-class.java"));
    }

    @Test
    public void asyncClientClassAwsJson() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(awsJsonServiceModels(), false);
        assertThat(asyncClientClass, generatesTo("test-aws-json-async-client-class.java"));

        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(awsJsonServiceModels(), true);
        assertThat(sraAsyncClientClass, generatesTo("sra/test-aws-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassAwsQueryCompatibleJson() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(awsQueryCompatibleJsonServiceModels());
        assertThat(asyncClientClass, generatesTo("test-aws-query-compatible-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassXml() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(xmlServiceModels(), false);
        assertThat(asyncClientClass, generatesTo("test-xml-async-client-class.java"));

        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(xmlServiceModels(), true);
        assertThat(sraAsyncClientClass, generatesTo("sra/test-xml-async-client-class.java"));
    }

    @Test
    public void asyncClientEndpointDiscovery() {
        AsyncClientClass asyncClientEndpointDiscovery = createAsyncClientClass(endpointDiscoveryModels());
        assertThat(asyncClientEndpointDiscovery, generatesTo("test-endpoint-discovery-async.java"));
    }

    @Test
    public void asyncClientCustomServiceMetaData() {
        AsyncClientClass asyncClientCustomServiceMetaData = createAsyncClientClass(customContentTypeModels());
        assertThat(asyncClientCustomServiceMetaData, generatesTo("test-customservicemetadata-async.java"));
    }

    @Test
    public void asyncClientCustomPackageName() {
        ClassSpec syncClientCustomServiceMetaData = createAsyncClientClass(customPackageModels());
        assertThat(syncClientCustomServiceMetaData, generatesTo("test-custompackage-async.java"));
    }

    @Test
    public void asyncClientClassRpcv2() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(rpcv2ServiceModels(), true);
        assertThat(asyncClientClass, generatesTo("test-rpcv2-async-client-class.java"));
    }

    @Test
    public void asyncClientBatchManager() {
        ClassSpec aSyncClientBatchManager = createAsyncClientClass(batchManagerModels());
        assertThat(aSyncClientBatchManager, generatesTo("test-batchmanager-async.java"));
    }

    private AsyncClientClass createAsyncClientClass(IntermediateModel model) {
        return new AsyncClientClass(GeneratorTaskParams.create(model, "sources/", "tests/", "resources/"));
    }

    private AsyncClientClass createAsyncClientClass(IntermediateModel model, boolean useSraAuth) {
        model.getCustomizationConfig().setUseSraAuth(useSraAuth);
        return createAsyncClientClass(model);
    }
}
