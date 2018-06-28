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

package software.amazon.awssdk.regions.internal.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
@ReviewBeforeRelease("Refactor to use SDK HTTP client instead of URL connection, also consider putting EC2MetadataClient into "
                     + "its own module")
public class ConnectionUtils {

    public static ConnectionUtils create() {
        return new ConnectionUtils();
    }

    public HttpURLConnection connectToEndpoint(URI endpoint, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection(Proxy.NO_PROXY);
        connection.setConnectTimeout(1000 * 2);
        connection.setReadTimeout(1000 * 5);
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        headers.forEach(connection::addRequestProperty);
        connection.setInstanceFollowRedirects(false);
        connection.connect();

        return connection;
    }

}
