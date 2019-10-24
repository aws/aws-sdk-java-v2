/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A dumbed-down version of Apache HTTP Components Client URIUtils.
 */
@SdkProtectedApi
public class UriUtils {

    private UriUtils() {
    }

    /**
     * Converts an absolute URI to a relative URI by stripping out the scheme,
     * authority, and fragment. In order to maintain compatibility with Apache
     * HTTP Client behavior, opaque and non-absolute URIs are returned as-is.
     * @param uri the uri
     * @return the relativized URI
     */
    public static URI relativize(URI uri) {
        Objects.requireNonNull(uri, "uri");
        if (uri.isOpaque()) {
            return uri;
        }
        try {
            return new URI(null, null, null, -1, StringUtils.isEmpty(uri.getPath()) ? "/" : uri.getPath(), uri.getQuery(), null);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }
}
