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

package software.amazon.awssdk.http;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface SdkHttpHeaders {

    /**
     * Returns the HTTP headers returned with this object.
     * <br/>
     * Should never be null, if there are no headers an empty map is returned
     *
     * @return The HTTP headers.
     */
    Map<String, List<String>> getHeaders();

    /**
     * Returns the list of values for a given header.
     * <br/>
     * Ignores the case of the header when performing the get
     * @param header the header to look for
     * @return the list of values associated with the header
     */
    default Collection<String> getValuesForHeader(String header) {
        return getHeaders().entrySet().stream()
                           .filter(e -> e.getKey().equalsIgnoreCase(header))
                           .flatMap(e -> e.getValue() != null ? e.getValue().stream() : Stream.empty())
                           .collect(toList());
    }

    /**
     * Gets the first value of the given header, if it's present.
     * <br/>
     * This is useful for headers like 'Content-Type' or
     * 'Content-Length' of which there is expected to be only one value if present.
     *
     * @param header Name of header to get first value for.
     * @return Empty optional if header is not present, otherwise fulfilled optional containing first value of the header.
     */
    default Optional<String> getFirstHeaderValue(String header) {
        return getValuesForHeader(header).stream().findFirst();
    }
}
