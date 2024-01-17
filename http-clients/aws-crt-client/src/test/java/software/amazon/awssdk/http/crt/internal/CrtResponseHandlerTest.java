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

package software.amazon.awssdk.http.crt.internal;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.mockito.Mockito;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.async.AsyncResponseHandler;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.crt.internal.response.CrtResponseAdapter;
import software.amazon.awssdk.http.crt.internal.response.InputStreamAdaptingHttpStreamResponseHandler;

public class CrtResponseHandlerTest extends BaseHttpStreamResponseHandlerTest {

    @Override
    HttpStreamResponseHandler responseHandler() {
        AsyncResponseHandler<Void> responseHandler = new AsyncResponseHandler<>((response,
                                                                                          executionAttributes) -> null, Function.identity(), new ExecutionAttributes());

        responseHandler.prepare();
        return CrtResponseAdapter.toCrtResponseHandler(crtConn, requestFuture, responseHandler);
    }
}
