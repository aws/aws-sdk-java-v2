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

package software.amazon.awssdk.http;

import java.io.IOException;
import java.util.concurrent.Callable;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * An HTTP request that can be invoked by {@link #call()}. Once invoked, the HTTP call can be cancelled via {@link #abort()},
 * which should release the thread that has invoked {@link #call()} as soon as possible.
 */
@SdkPublicApi
public interface ExecutableHttpRequest extends Callable<HttpExecuteResponse>, Abortable {
    @Override
    HttpExecuteResponse call() throws IOException;
}
