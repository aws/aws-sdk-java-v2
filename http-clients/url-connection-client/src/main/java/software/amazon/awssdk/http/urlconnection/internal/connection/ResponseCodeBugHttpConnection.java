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

package software.amazon.awssdk.http.urlconnection.internal.connection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An implementation of {@link HttpConnection} that handles a bug in the default {@link HttpURLConnection} where it can throw
 * a {@link NullPointerException}s for reasons that still require further investigation, but are assumed to be due to a
 * bug in the JDK. Propagating such NPEs is confusing for users and are not subject to being retried on by the default
 * retry policy configuration, so instead we bias towards propagating these as {@link IOException}s.
 *
 * <p>TODO: Determine precise root cause of intermittent NPEs and submit JDK bug report if applicable.
 */
@SdkInternalApi
public class ResponseCodeBugHttpConnection extends DelegatingHttpConnection {
    public ResponseCodeBugHttpConnection(HttpConnection delegate) {
        super(delegate);
    }

    @Override
    public int getResponseCode() {
        try {
            return delegate.getResponseCode();
        } catch (NullPointerException e) {
            throw new UncheckedIOException(new IOException("Unexpected NullPointerException when trying to read response from "
                                                           + "HttpURLConnection", e));
        }
    }
}
