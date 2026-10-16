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

package software.amazon.awssdk.http.crt;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

class AwsCrtHttpClientTrustStoreSystemPropertiesTest extends CrtTrustStoreSystemPropertiesTestSuite<SdkHttpClient> {

    @Override
    protected SdkHttpClient buildClient(Boolean useTrustStoreSystemProperties) {
        return AwsCrtHttpClient.builder()
                               .useTrustStoreSystemProperties(useTrustStoreSystemProperties)
                               .connectionAcquisitionTimeout(CONNECTION_ACQUISITION_TIMEOUT)
                               .build();
    }

    @Override
    protected SdkHttpClient buildClientTrustingAllCertificates(Boolean useTrustStoreSystemProperties) {
        return AwsCrtHttpClient.builder()
                               .useTrustStoreSystemProperties(useTrustStoreSystemProperties)
                               .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }

    @Override
    protected SdkHttpClient buildClientWithPrivateEventLoopGroup(Boolean useTrustStoreSystemProperties) {
        return AwsCrtHttpClient.builder()
                               .numEventLoopThreads(2)
                               .useTrustStoreSystemProperties(useTrustStoreSystemProperties)
                               .build();
    }

    @Override
    protected void executeGet(SdkHttpClient client, URI uri) throws Exception {
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(createRequest(uri))
                                                                               .build())
                                             .call();
        response.responseBody().ifPresent(body -> invokeSafely(body::close));
    }
}
