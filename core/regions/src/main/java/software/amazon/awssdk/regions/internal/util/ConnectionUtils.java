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

package software.amazon.awssdk.regions.internal.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.internal.SystemSettingUtils;


@SdkInternalApi
//TODO: Refactor to use SDK HTTP client instead of URL connection, also consider putting EC2MetadataClient in its own module
public class ConnectionUtils {


    private final Lazy<Integer> metadataServiceTimeoutMillis = new Lazy<>(this::resolveMetadataServiceTimeoutMillis);

    public static ConnectionUtils create() {
        return new ConnectionUtils();
    }

    public HttpURLConnection connectToEndpoint(URI endpoint, Map<String, String> headers) throws IOException {
        return connectToEndpoint(endpoint, headers, "GET");
    }

    public HttpURLConnection connectToEndpoint(URI endpoint,
                                               Map<String, String> headers,
                                               String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection(Proxy.NO_PROXY);

        int timeoutMillis = metadataServiceTimeoutMillis.getValue();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);

        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        headers.forEach(connection::addRequestProperty);
        connection.setInstanceFollowRedirects(false);
        connection.connect();

        return connection;
    }

    private int resolveMetadataServiceTimeoutMillis() {

        String timeoutValue = SystemSettingUtils.resolveSetting(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT)
                                                 .orElseGet(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT::defaultValue);

        try {
            // To match the CLI behavior, support both integers and doubles; try int first for exact values, fall back to double.
            int timeoutSeconds = Integer.parseInt(timeoutValue);
            return NumericUtils.saturatedCast(Duration.ofSeconds(timeoutSeconds).toMillis());
        } catch (NumberFormatException e) {
            try {
                // Fallback to parsing the timeout as a double (seconds) and convert to milliseconds
                double timeoutSeconds = Double.parseDouble(timeoutValue);
                return NumericUtils.saturatedCast(Math.round(timeoutSeconds * 1000));
            } catch (NumberFormatException ignored) {
                throw new IllegalStateException(String.format(
                    "%s environment variable value '%s' is not a valid integer or double.",
                    SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property(),
                    timeoutValue
                ));
            }
        }
    }
}
