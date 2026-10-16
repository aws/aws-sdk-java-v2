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

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.http.HttpTestUtils;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

class AwsCrtAsyncHttpClientTrustStoreSystemPropertiesTest
    extends CrtTrustStoreSystemPropertiesTestSuite<SdkAsyncHttpClient> {

    @Override
    protected SdkAsyncHttpClient buildClient(Boolean useTrustStoreSystemProperties) {
        return AwsCrtAsyncHttpClient.builder()
                                    .useTrustStoreSystemProperties(useTrustStoreSystemProperties)
                                    .connectionAcquisitionTimeout(CONNECTION_ACQUISITION_TIMEOUT)
                                    .build();
    }

    @Override
    protected SdkAsyncHttpClient buildClientTrustingAllCertificates(Boolean useTrustStoreSystemProperties) {
        return AwsCrtAsyncHttpClient.builder()
                                    .useTrustStoreSystemProperties(useTrustStoreSystemProperties)
                                    .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }

    @Override
    protected SdkAsyncHttpClient buildClientWithPrivateEventLoopGroup(Boolean useTrustStoreSystemProperties) {
        return AwsCrtAsyncHttpClient.builder()
                                    .numEventLoopThreads(2)
                                    .useTrustStoreSystemProperties(useTrustStoreSystemProperties)
                                    .build();
    }

    @Override
    protected void executeGet(SdkAsyncHttpClient client, URI uri) throws Exception {
        try {
            HttpTestUtils.sendRequest(client, createRequest(uri)).get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }
}
