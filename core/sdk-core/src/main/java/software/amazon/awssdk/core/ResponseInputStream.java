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

package software.amazon.awssdk.core;

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.io.SdkFilterInputStream;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Input stream that provides access to the unmarshalled POJO response returned by the service in addition to the streamed
 * contents. This input stream should be closed after all data has been read from the stream.
 * <p>
 * Note about the Apache http client: This input stream can be used to leverage a feature of the Apache http client where
 * connections are released back to the connection pool to be reused. As such, calling {@link ResponseInputStream#close() close}
 * on this input stream will result in reading the remaining data from the stream and leaving the connection open, even if the
 * stream was only partially read from. For large http payload, this means reading <em>all</em> of the http body before releasing
 * the connection which may add latency.
 * <p>
 * If it is not desired to read remaining data from the stream, you can explicitly abort the connection via {@link #abort()}
 * instead. This will close the underlying connection and require establishing a new HTTP connection on subsequent requests which
 * may outweigh the cost of reading the additional data.
 * <p>
 * The Url Connection and Crt http clients are not subject to this behaviour so the {@link ResponseInputStream#close() close} and
 * {@link ResponseInputStream#abort() abort} methods will behave similarly with them.
 */
@SdkPublicApi
public final class ResponseInputStream<ResponseT> extends SdkFilterInputStream implements Abortable {

    private final ResponseT response;
    private final Abortable abortable;

    public ResponseInputStream(ResponseT resp, AbortableInputStream in) {
        super(in);
        this.response = Validate.paramNotNull(resp, "response");
        this.abortable = Validate.paramNotNull(in, "abortableInputStream");
    }

    public ResponseInputStream(ResponseT resp, InputStream in) {
        super(in);
        this.response = Validate.paramNotNull(resp, "response");
        this.abortable = in instanceof Abortable ? (Abortable) in : null;
    }

    /**
     * @return The unmarshalled POJO response associated with this content.
     */
    public ResponseT response() {
        return response;
    }

    /**
     * Close the underlying connection, dropping all remaining data in the stream, and not leaving the
     * connection open to be used for future requests.
     */
    @Override
    public void abort() {
        if (abortable != null) {
            abortable.abort();
        }
        IoUtils.closeQuietly(in, null);
    }
}
