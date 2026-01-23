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

package software.amazon.awssdk.services.s3.internal.plugins;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

@WireMockTest
class ListOfStringsAuthParamPluginTest {

    private static S3AsyncClient s3Client;
    private ListOfStringsParamPlugin plugin;

    @BeforeEach
    public void init(WireMockRuntimeInfo wm) {
        plugin = new ListOfStringsParamPlugin();

        AwsBasicCredentials credentials = AwsBasicCredentials.create("key", "secret");
        s3Client = S3AsyncClient.builder()
                                .region(Region.US_EAST_1)
                                .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                .addPlugin(plugin)
                                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                .build();
    }

    @Test
    void callingDeleteObjects_requestWithCompleteListOfKey_returnsRightValues() {
        s3Client.deleteObjects(r -> r.bucket("test").delete(o -> o.objects(
            ObjectIdentifier.builder().key("x").versionId("1").build(),
            ObjectIdentifier.builder().key("y").versionId("2").build()
        )));
        assertThat(plugin.storedKeys()).isEqualTo(asList("x", "y"));
    }

    @Test
    void callingDeleteObjects_requestWithInCompleteListOfKey_returnsRightValues() {
        s3Client.deleteObjects(r -> r.bucket("test").delete(o -> o.objects(
            ObjectIdentifier.builder().versionId("1").build(),
            ObjectIdentifier.builder().key("y").versionId("2").build()
        )));
        assertThat(plugin.storedKeys()).isEqualTo(asList("y"));
    }

    @Test
    void callingDeleteObjects_requestWithNoList_returnsRightValues() {
        s3Client.deleteObjects(DeleteObjectsRequest.builder().delete(Delete.builder().build()).build());
        assertThat(plugin.storedKeys()).asList().isEmpty();
    }

    @Test
    void callingDeleteObjects_requestWithoutEmptyList_returnsRightValues() {
        s3Client.deleteObjects(DeleteObjectsRequest.builder().delete(Delete.builder().objects(Collections.emptyList()).build()).build());
        assertThat(plugin.storedKeys()).asList().isEmpty();
    }

    @Test
    void callingDeleteObjects_requestWithListButNoKey_returnsRightValues() {
        List<ObjectIdentifier> objects = asList(ObjectIdentifier.builder().build());
        s3Client.deleteObjects(DeleteObjectsRequest.builder().delete(Delete.builder().objects(objects).build()).build());
        assertThat(plugin.storedKeys()).asList().isEmpty();
    }

    private static class ListOfStringsParamPlugin implements SdkPlugin {

        private ListOfStringsParamAuthSchemeProvider localProvider;

        @Override
        public void configureClient(SdkServiceClientConfiguration.Builder config) {
            S3ServiceClientConfiguration.Builder serviceClientConfiguration = (S3ServiceClientConfiguration.Builder) config;
            S3AuthSchemeProvider defaultProvider = serviceClientConfiguration.authSchemeProvider();
            localProvider = new ListOfStringsParamAuthSchemeProvider(defaultProvider);
            serviceClientConfiguration.authSchemeProvider(localProvider);
        }

        List<String> storedKeys() {
            return localProvider.storedKeys;
        }

        private static class ListOfStringsParamAuthSchemeProvider implements S3AuthSchemeProvider {

            private List<String> storedKeys;
            S3AuthSchemeProvider delegate;

            public ListOfStringsParamAuthSchemeProvider(S3AuthSchemeProvider authSchemeProvider) {
                this.delegate = authSchemeProvider;
            }

            @Override
            public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
                List<AuthSchemeOption> availableAuthSchemes = delegate.resolveAuthScheme(authSchemeParams);

                List<String> keys = authSchemeParams.deleteObjectKeys();
                if (keys != null) {
                    storedKeys = keys;
                }
                return availableAuthSchemes;
            }
        }
    }

}
