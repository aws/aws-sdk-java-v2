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

package software.amazon.awssdk.regions.providers;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.ToString;

/**
 * A wrapper for {@link AwsRegionProvider} that defers creation of the underlying provider until the first time the
 * {@link AwsRegionProvider#getRegion()} method is invoked.
 */
@SdkProtectedApi
public class LazyAwsRegionProvider implements AwsRegionProvider {
    private final Supplier<AwsRegionProvider> delegateConstructor;
    private volatile AwsRegionProvider delegate;

    public LazyAwsRegionProvider(Supplier<AwsRegionProvider> delegateConstructor) {
        this.delegateConstructor = delegateConstructor;
    }

    @Override
    public Region getRegion() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = delegateConstructor.get();
                }
            }
        }
        return delegate.getRegion();
    }

    @Override
    public String toString() {
        return ToString.builder("LazyAwsRegionProvider")
                       .add("delegateConstructor", delegateConstructor)
                       .add("delegate", delegate)
                       .build();
    }
}
