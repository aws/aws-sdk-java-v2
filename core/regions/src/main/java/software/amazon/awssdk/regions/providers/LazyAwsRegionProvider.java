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
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.ToString;

/**
 * A wrapper for {@link AwsRegionProvider} that defers creation of the underlying provider until the first time the
 * {@link AwsRegionProvider#getRegion()} method is invoked.
 */
@SdkProtectedApi
public class LazyAwsRegionProvider implements AwsRegionProvider {
    private final Lazy<AwsRegionProvider> delegate;

    public LazyAwsRegionProvider(Supplier<AwsRegionProvider> delegateConstructor) {
        this.delegate = new Lazy<>(delegateConstructor);
    }

    @Override
    public Region getRegion() {
        return delegate.getValue().getRegion();
    }

    @Override
    public String toString() {
        return ToString.builder("LazyAwsRegionProvider")
                       .add("delegate", delegate)
                       .build();
    }
}
