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

package software.amazon.awssdk.core.internal.http.loader;

import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;

/**
 * {@link HttpWarmupInvoker} that owns the set of {@link HttpClientWarmer}s and invokes each.
 */
@SdkInternalApi
public final class ClasspathHttpWarmupInvoker implements HttpWarmupInvoker {

    private final List<HttpClientWarmer> warmers;

    @SdkTestInternalApi
    ClasspathHttpWarmupInvoker(List<HttpClientWarmer> warmers) {
        this.warmers = warmers;
    }

    /**
     * @return an invoker over the HTTP-client warmers on the classpath.
     */
    public static HttpWarmupInvoker create() {
        return new ClasspathHttpWarmupInvoker(
            Arrays.asList(SyncHttpClientWarmer.create(), AsyncHttpClientWarmer.create()));
    }

    @Override
    public void invokeAll() {
        for (HttpClientWarmer warmer : warmers) {
            warmer.warmAll();
        }
    }
}
