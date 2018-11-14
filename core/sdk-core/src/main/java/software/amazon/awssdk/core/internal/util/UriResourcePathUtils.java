/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.util;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class UriResourcePathUtils {

    private UriResourcePathUtils() {
    }

    /**
     * Creates a new {@link URI} from the given URI by replacing the host value.
     * @param uri Original URI
     * @param newHostPrefix New host for the uri
     */
    public static URI updateUriHost(URI uri, String newHostPrefix) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), newHostPrefix + uri.getHost(),
                           uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
