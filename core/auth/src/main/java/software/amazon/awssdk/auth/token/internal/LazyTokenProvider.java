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

package software.amazon.awssdk.auth.token.internal;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;

/**
 * A wrapper for {@link SdkTokenProvider} that defers creation of the underlying provider until the first time the
 * {@link SdkTokenProvider#resolveToken()} method is invoked.
 */
@SdkInternalApi
public class LazyTokenProvider implements SdkTokenProvider, SdkAutoCloseable {
    private final Lazy<SdkTokenProvider> delegate;

    public LazyTokenProvider(Supplier<SdkTokenProvider> delegateConstructor) {
        this.delegate = new Lazy<>(delegateConstructor);
    }

    public static LazyTokenProvider create(Supplier<SdkTokenProvider> delegateConstructor) {
        return new LazyTokenProvider(delegateConstructor);
    }

    @Override
    public SdkToken resolveToken() {
        return delegate.getValue().resolveToken();
    }

    @Override
    public void close() {
        IoUtils.closeIfCloseable(delegate, null);
    }

    @Override
    public String toString() {
        return ToString.builder("LazyTokenProvider")
                       .add("delegate", delegate)
                       .build();
    }
}
