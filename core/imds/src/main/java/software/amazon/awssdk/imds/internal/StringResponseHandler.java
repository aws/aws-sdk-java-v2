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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class StringResponseHandler implements HttpResponseHandler<String> {
    private static final Logger log = Logger.loggerFor(StringResponseHandler.class);

    private CompletableFuture<String> future;

    public void setFuture(CompletableFuture<String> future) {
        this.future = future;
    }

    @Override
    public String handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        HttpStatusFamily statusCode = HttpStatusFamily.of(response.statusCode());
        AbortableInputStream inputStream = response
            .content().orElseThrow(() -> SdkClientException.create("Unexpected error: empty response content"));
        String responseContent = uncheckedInputStreamToUtf8(inputStream);
        if (statusCode.isOneOf(HttpStatusFamily.CLIENT_ERROR)) {
            // non-retryable error
            Supplier<String> msg = () -> String.format("Error while executing EC2Metadata request: received http status %d",
                                                       response.statusCode());
            log.info(msg);
            future.completeExceptionally(SdkClientException.create(responseContent));
        } else if (statusCode.isOneOf(HttpStatusFamily.SERVER_ERROR)) {
            // retryable error
            Supplier<String> msg = () -> String.format("Error while executing EC2Metadata request: received http status %d",
                                                       response.statusCode());
            log.info(msg);
            future.completeExceptionally(RetryableException.create(responseContent));
        }
        return responseContent;
    }

    protected static String uncheckedInputStreamToUtf8(AbortableInputStream inputStream) {
        try {
            return IoUtils.toUtf8String(inputStream);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        } finally {
            IoUtils.closeQuietly(inputStream, log.logger());
        }
    }

}
