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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;

@SdkInternalApi
public final class TokenResponseHandler implements HttpResponseHandler<Token> {

    private final StringResponseHandler delegateHandler;

    public TokenResponseHandler() {
        this.delegateHandler = new StringResponseHandler();
    }

    public void setFuture(CompletableFuture<?> future) {
        this.delegateHandler.setFuture(future);
    }

    @Override
    public Token handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        String tokenValue = delegateHandler.handle(response, executionAttributes);
        Optional<String> ttl = response.firstMatchingHeader(EC2_METADATA_TOKEN_TTL_HEADER);

        if (!ttl.isPresent()) {
            delegateHandler.getFuture().completeExceptionally(
                RetryableException.create(EC2_METADATA_TOKEN_TTL_HEADER + " header not found in token response"));
            return null;
        }
        Duration ttlDuration;
        try {
            ttlDuration = Duration.ofSeconds(Long.parseLong(ttl.get()));
        } catch (NumberFormatException nfe) {
            delegateHandler.getFuture().completeExceptionally(
                RetryableException.create("Invalid token format received from IMDS server", nfe));
            return null;
        }
        return new Token(tokenValue, ttlDuration);
    }
}
