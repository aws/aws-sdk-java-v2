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

package software.amazon.awssdk.imds.internal;

import static software.amazon.awssdk.imds.internal.RequestMarshaller.EC2_METADATA_TOKEN_TTL_HEADER;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class TokenResponseHandler implements HttpResponseHandler<Token> {
    private static final Logger log = Logger.loggerFor(TokenResponseHandler.class);

    private CompletableFuture<Token> future;
    private final long ttlSeconds;

    private StringResponseHandler delegateHandler;

    public TokenResponseHandler(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
        this.delegateHandler = new StringResponseHandler();
    }

    public void setFuture(CompletableFuture<?> future) {
        this.delegateHandler.setFuture(future);
    }

    @Override
    public Token handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        String tokenValue = delegateHandler.handle(response, executionAttributes);
        Duration ttl = response.firstMatchingHeader(EC2_METADATA_TOKEN_TTL_HEADER)
                               .map(Long::parseLong)
                               .map(Duration::ofSeconds)
                               .orElseGet(() -> Duration.ofSeconds(ttlSeconds));
        return new Token(tokenValue, ttl);
    }

}
