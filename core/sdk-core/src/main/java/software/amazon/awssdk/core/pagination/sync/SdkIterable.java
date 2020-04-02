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

package software.amazon.awssdk.core.pagination.sync;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A custom iterable used in paginated responses.
 *
 * This interface has a default stream() method which creates a stream from
 * spliterator method.
 *
 * @param <T> the type of elements returned by the iterator
 */
@SdkPublicApi
public interface SdkIterable<T> extends Iterable<T> {

    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
