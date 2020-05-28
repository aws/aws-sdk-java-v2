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

package software.amazon.awssdk.services.retry;

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;

public class AsyncClientRetryModeTest
    extends ClientRetryModeTestSuite<ProtocolRestJsonAsyncClient, ProtocolRestJsonAsyncClientBuilder> {
    @Override
    protected ProtocolRestJsonAsyncClientBuilder newClientBuilder() {
        return ProtocolRestJsonAsyncClient.builder();
    }

    @Override
    protected AllTypesResponse callAllTypes(ProtocolRestJsonAsyncClient client) {
        try {
            return client.allTypes().join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }

            throw e;
        }
    }
}
