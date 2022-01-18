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

package software.amazon.awssdk.http.apache.internal;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultClientConnectionReuseStrategy;
import org.apache.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Do not reuse connections that returned a 5xx error.
 *
 * <p>This is not strictly the behavior we would want in an AWS client, because sometimes we might want to keep a connection open
 * (e.g. an undocumented service's 503 'SlowDown') and sometimes we might want to close the connection (e.g. S3's 400
 * RequestTimeout or Glacier's 408 RequestTimeoutException), but this is good enough for the majority of services, and the ones
 * for which it is not should not be impacted too harshly.
 */
@SdkInternalApi
public class SdkConnectionReuseStrategy extends DefaultClientConnectionReuseStrategy {
    @Override
    public boolean keepAlive(HttpResponse response, HttpContext context) {
        if (!super.keepAlive(response, context)) {
            return false;
        }

        if (response == null || response.getStatusLine() == null) {
            return false;
        }

        return !is500(response);
    }

    private boolean is500(HttpResponse httpResponse) {
        return httpResponse.getStatusLine().getStatusCode() / 100 == 5;
    }
}
