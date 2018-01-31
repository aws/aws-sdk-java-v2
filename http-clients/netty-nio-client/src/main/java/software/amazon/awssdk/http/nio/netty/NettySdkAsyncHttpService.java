/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty;

import software.amazon.awssdk.http.async.SdkAsyncHttpClientFactory;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;

/**
 * Service binding for the Netty default implementation. Allows SDK to pick this up automatically from the classpath.
 */
public class NettySdkAsyncHttpService implements SdkAsyncHttpService {

    @Override
    public SdkAsyncHttpClientFactory createAsyncHttpClientFactory() {
        return NettySdkHttpClientFactory.builder().build();
    }
}
