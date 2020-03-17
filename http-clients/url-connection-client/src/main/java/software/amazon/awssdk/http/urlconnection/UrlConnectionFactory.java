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

package software.amazon.awssdk.http.urlconnection;

import java.net.HttpURLConnection;
import java.net.URI;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * An interface that, given a {@link URI} creates a new {@link HttpURLConnection}. This allows customization
 * of the creation and configuration of the {@link HttpURLConnection}.
 */
@FunctionalInterface
@SdkPublicApi
public interface UrlConnectionFactory {

    /**
     * For the given {@link URI} create an {@link HttpURLConnection}.
     * @param uri the {@link URI} of the request
     * @return a {@link HttpURLConnection} to the given {@link URI}
     */
    HttpURLConnection createConnection(URI uri);
}
