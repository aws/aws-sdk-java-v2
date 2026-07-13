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

package software.amazon.awssdk.core.internal.crac;

import java.util.Collections;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.utils.ToString;

/**
 * Outcome of a {@link TargetedWarmUpInvoker} run: the client types that matched and the client names that warmed
 * successfully.
 */
@SdkInternalApi
public final class TargetedWarmUpResult {

    private final Set<ClientType> matchedClientTypes;
    private final Set<String> warmedClientNames;

    TargetedWarmUpResult(Set<ClientType> matchedClientTypes, Set<String> warmedClientNames) {
        this.matchedClientTypes = Collections.unmodifiableSet(matchedClientTypes);
        this.warmedClientNames = Collections.unmodifiableSet(warmedClientNames);
    }

    public Set<ClientType> matchedClientTypes() {
        return matchedClientTypes;
    }

    public Set<String> warmedClientNames() {
        return warmedClientNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TargetedWarmUpResult that = (TargetedWarmUpResult) o;

        if (!matchedClientTypes.equals(that.matchedClientTypes)) {
            return false;
        }
        return warmedClientNames.equals(that.warmedClientNames);
    }

    @Override
    public int hashCode() {
        int result = matchedClientTypes.hashCode();
        result = 31 * result + warmedClientNames.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("TargetedWarmUpResult")
                       .add("matchedClientTypes", matchedClientTypes)
                       .add("warmedClientNames", warmedClientNames)
                       .build();
    }
}
