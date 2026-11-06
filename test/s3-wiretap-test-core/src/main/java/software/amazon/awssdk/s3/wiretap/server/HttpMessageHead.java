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

package software.amazon.awssdk.s3.wiretap.server;

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.s3.wiretap.helpers.CaseInsensitiveMap;

public interface HttpMessageHead {

    CaseInsensitiveMap<List<String>> headers();

    default String header(String name) {
        List<String> v = headers().get(name);
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("No header named " + name);
        }
        if (v.size() > 1) {
            throw new IllegalArgumentException("Multiple headers named " + name + ": " + v);
        }
        return v.getFirst();
    }

    default Optional<String> optionalHeader(String name) {
        List<String> v = headers().get(name);
        return v == null || v.isEmpty() ? Optional.empty() : Optional.of(v.getFirst());
    }

    default boolean hasHeader(String name) {
        return headers().containsKey(name);
    }
}
