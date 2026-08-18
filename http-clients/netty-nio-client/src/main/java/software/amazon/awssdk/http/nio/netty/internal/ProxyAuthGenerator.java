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

package software.amazon.awssdk.http.nio.netty.internal;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.ProxyAuthScheme;

/**
 * Generates the auth params for an {@code Authorization} HTTP header.
 */
@SdkInternalApi
public interface ProxyAuthGenerator {
    /**
     * The name of the auth scheme this generator supports.
     */
    ProxyAuthScheme scheme();

    /**
     * Generate the auth params for this request.
     * <p>
     * This is asynchronous because generating the params may block - Kerberos, for example, may need to read the ticket cache
     * from disk and contact the KDC. Implementations that block MUST complete the returned future from a thread other than the
     * caller's; the caller is a Netty event loop thread, and blocking it would stall every other channel assigned to that loop.
     */
    CompletableFuture<String> generateAuthParams(URI proxyEndpoint);
}
