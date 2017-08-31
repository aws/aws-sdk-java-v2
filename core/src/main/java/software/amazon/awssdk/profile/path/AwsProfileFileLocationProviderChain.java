/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.profile.path;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Delegates to a chain of {@link AwsProfileFileLocationProvider}. Returns null if no provider in
 * the chain can come up with a location to the shared credentials file.
 */
@SdkInternalApi
public class AwsProfileFileLocationProviderChain implements AwsProfileFileLocationProvider {

    private final List<AwsProfileFileLocationProvider> providers = new ArrayList<AwsProfileFileLocationProvider>();

    public AwsProfileFileLocationProviderChain(AwsProfileFileLocationProvider... providers) {
        Collections.addAll(this.providers, providers);
    }

    @Override
    public Optional<Path> getLocation() {
        for (AwsProfileFileLocationProvider provider : providers) {
            Optional<Path> path = provider.getLocation();
            if (path.isPresent()) {
                return path;
            }
        }
        return Optional.empty();
    }
}
