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

package software.amazon.awssdk.endpoints;

import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A key for adding and retrieving attributes from an {@link Endpoint} in a typesafe manner.
 * @param <T> The type of the attribute.
 */
@SdkPublicApi
public final class EndpointAttributeKey<T> {
    private final String name;
    private final Class<T> clzz;

    public EndpointAttributeKey(String name, Class<T> clzz) {
        this.name = name;
        this.clzz = clzz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EndpointAttributeKey<?> that = (EndpointAttributeKey<?>) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return clzz != null ? clzz.equals(that.clzz) : that.clzz == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (clzz != null ? clzz.hashCode() : 0);
        return result;
    }

    public static <E> EndpointAttributeKey<List<E>> forList(String name) {
        return new EndpointAttributeKey(name, List.class);
    }
}
