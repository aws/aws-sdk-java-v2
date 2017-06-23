/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.client;

import software.amazon.awssdk.ServiceAdvancedConfiguration;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.annotation.SdkProtectedApi;

/**
 * Client level parameters for the {@link ClientHandler} implementation.
 */
@SdkProtectedApi
@NotThreadSafe
public class ClientHandlerParams {

    private AwsSyncClientParams clientParams;
    private ServiceAdvancedConfiguration serviceAdvancedConfiguration;

    private AwsAsyncClientParams asyncClientParams;

    /**
     * By default, the CRC 32 checksum is calculated based on the uncompressed data.
     */
    private boolean crc32FromCompressedDataEnabled = false;

    public AwsSyncClientParams getClientParams() {
        return clientParams;
    }

    public ClientHandlerParams withClientParams(AwsSyncClientParams clientParams) {
        this.clientParams = clientParams;
        return this;
    }

    public AwsAsyncClientParams getAsyncClientParams() {
        return asyncClientParams;
    }

    public ClientHandlerParams withAsyncClientParams(AwsAsyncClientParams clientParams) {
        this.asyncClientParams = clientParams;
        return this;
    }

    public ServiceAdvancedConfiguration getServiceAdvancedConfiguration() {
        return serviceAdvancedConfiguration;
    }

    public ClientHandlerParams withServiceAdvancedConfiguration(ServiceAdvancedConfiguration serviceAdvancecConfiguration) {
        this.serviceAdvancedConfiguration = serviceAdvancecConfiguration;
        return this;
    }

    public boolean isCalculateCrc32FromCompressedDataEnabled() {
        return crc32FromCompressedDataEnabled;
    }

    public ClientHandlerParams withCalculateCrc32FromCompressedDataEnabled(boolean crc32FromCompressedDataEnabled) {
        this.crc32FromCompressedDataEnabled = crc32FromCompressedDataEnabled;
        return this;
    }
}
