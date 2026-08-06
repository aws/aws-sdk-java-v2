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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.awsJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.awsQueryCompatibleJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.batchManagerModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.cborServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.customContentTypeModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.customPackageModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.endpointDiscoveryModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.opsWithSigv4a;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.presignedUrlExtensionModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.serviceWithCustomContextParamsModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.rpcv2ServiceModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.xmlServiceModels;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import org.junit.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class AsyncClientClassTest {
    @Test
    public void asyncClientClassRestJson() {
        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(restJsonServiceModels());
        assertThat(sraAsyncClientClass, generatesTo("test-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassQuery() {
        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(queryServiceModels());
        assertThat(sraAsyncClientClass, generatesTo("test-query-async-client-class.java"));
    }

    @Test
    public void asyncClientClassAwsJson() {
        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(awsJsonServiceModels());
        assertThat(sraAsyncClientClass, generatesTo("test-aws-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassCbor() {
        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(cborServiceModels());
        assertThat(sraAsyncClientClass, generatesTo("test-cbor-async-client-class.java"));
    }

    @Test
    public void asyncClientClassAwsQueryCompatibleJson() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(awsQueryCompatibleJsonServiceModels());
        assertThat(asyncClientClass, generatesTo("test-aws-query-compatible-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassXml() {
        AsyncClientClass sraAsyncClientClass = createAsyncClientClass(xmlServiceModels());
        assertThat(sraAsyncClientClass, generatesTo("test-xml-async-client-class.java"));
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
        AsyncClientClass asyncClientClass = createAsyncClientClass(rpcv2ServiceModels());
        assertThat(asyncClientClass, generatesTo("test-rpcv2-async-client-class.java"));
    }

    @Test
    public void asyncClientBatchManager() {
        ClassSpec aSyncClientBatchManager = createAsyncClientClass(batchManagerModels());
        assertThat(aSyncClientBatchManager, generatesTo("test-batchmanager-async.java"));
    }
    
    @Test
    public void asyncClientPresignedUrlExtension() {
        ClassSpec asyncClientPresignedUrlExtension = createAsyncClientClass(presignedUrlExtensionModels());
        assertThat(asyncClientPresignedUrlExtension, generatesTo("test-presignedurl-async.java"));
    }

    @Test
    public void asyncClientWithStreamingUnsignedPayload() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(opsWithSigv4a());
        assertThat(asyncClientClass, generatesTo("test-unsigned-payload-trait-async-client-class.java"));
    }

    @Test
    public void asyncClientWithCustomContextParams() {
        AsyncClientClass asyncClientClass = createAsyncClientClass(serviceWithCustomContextParamsModels());
        assertThat(asyncClientClass, generatesTo("test-custom-context-params-async-client-class.java"));
    }

    @Test
    public void asyncClientEndpointDiscovery_whenRequiredEndpointDiscoveryAllowsEndpointOverride_generatesFallbackAndWarning()
        throws IOException {
        IntermediateModel model = endpointDiscoveryModels();
        model.getCustomizationConfig().setAllowEndpointOverrideForEndpointDiscoveryRequiredOperations(true);

        String generatedClient = generateClass(createAsyncClientClass(model));

        String requiredOperationOverrideFallback =
            "if (endpointOverridden) {\n"
            + "        endpointDiscoveryEnabled = false;\n"
            + "      } else if (!endpointDiscoveryEnabled) {\n"
            + "        throw new IllegalStateException(\"This operation requires endpoint discovery to be enabled, or for "
            + "you to specify an endpoint override when the client is created.\");\n"
            + "      }";
        String constructorWarning =
            "if (clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).isEndpointOverridden()) {\n"
            + "        log.warn(\"Endpoint discovery is enabled for this client, and an endpoint override was also specified. "
            + "This will disable endpoint discovery for methods that require it, instead using the specified endpoint override. "
            + "This may or may not be what you intended.\");\n"
            + "      }";

        assertThat(generatedClient, containsString(requiredOperationOverrideFallback));
        assertThat(generatedClient, containsString(constructorWarning));
    }

    @Test
    public void asyncClientClass_withUtilitiesInstanceTypeAndExistingScheduledExecutor_addsExpectedCode() {
        IntermediateModel utilitiesModel = awsJsonServiceModels();
        utilitiesModel.getCustomizationConfig()
                      .getUtilitiesMethod()
                      .setInstanceType("software.amazon.awssdk.services.json.DefaultJsonUtilities");
        AsyncClientClass asyncClientClassWithUtilities = createAsyncClientClass(utilitiesModel);

        MethodSpec utilitiesMethod = asyncClientClassWithUtilities.utilitiesMethod();

        String expectedUtilitiesStatement =
            "return software.amazon.awssdk.services.json.DefaultJsonUtilities.create(param1,param2,param3);";
        assertThat(utilitiesMethod.toString(), containsString(expectedUtilitiesStatement));

        AsyncClientClass asyncClientClassWithWaiters = createAsyncClientClass(queryServiceModels());
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder("Test");

        asyncClientClassWithWaiters.addFields(typeBuilder);
        asyncClientClassWithWaiters.addFields(typeBuilder);

        long scheduledExecutorFieldCount = typeBuilder.build()
                                                      .fieldSpecs
                                                      .stream()
                                                      .filter(f -> f.name.equals("executorService"))
                                                      .count();
        assertThat(scheduledExecutorFieldCount, equalTo(1L));
    }

    private AsyncClientClass createAsyncClientClass(IntermediateModel model) {
        return new AsyncClientClass(GeneratorTaskParams.create(model, "sources/", "tests/", "resources/"));
    }

    private String generateClass(ClassSpec spec) throws IOException {
        StringBuilder output = new StringBuilder();
        PoetUtils.buildJavaFile(spec).writeTo(output);
        return output.toString();
    }
}
