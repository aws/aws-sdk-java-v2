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

package software.amazon.awssdk.s3.mockserver.client;

import software.amazon.awssdk.s3.mockserver.client.feature.Feature;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

public final class ConfiguredClient {

    private final S3Client client;
    private final S3AsyncClient asyncClient;
    private final ClientConfig config;

    public ConfiguredClient(S3Client client, S3AsyncClient asyncClient, ClientConfig config) {
        this.client = client;
        this.asyncClient = asyncClient;
        this.config = config;
    }

    public S3Client client() {
        return client;
    }

    public S3AsyncClient asyncClient() {
        return asyncClient;
    }

    public ClientConfig config() {
        return config;
    }

    public <T, F extends Feature<T>> T value(Class<F> featureType) {
        return config.value(featureType);
    }

    @Override
    public String toString() {
        return "ConfiguredClient[" + config.describe() + "]";
    }
}
