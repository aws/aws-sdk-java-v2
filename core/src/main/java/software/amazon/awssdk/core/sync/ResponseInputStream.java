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

package software.amazon.awssdk.core.sync;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.runtime.io.SdkFilterInputStream;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;

/**
 * Input stream that provides access to the unmarshalled POJO response returned by the service in addition to the streamed
 * contents. This input stream should be closed to release the underlying connection back to the connection pool.
 *
 * <p>
 * If it is not desired to read remaining data from the stream, you can explicitly abort the connection via {@link #abort()}.
 * Note that this will close the underlying connection and require establishing an HTTP connection which may outweigh the
 * cost of reading the additional data.
 * </p>
 */
public final class ResponseInputStream<ResponseT> extends SdkFilterInputStream implements Abortable {

    private final ResponseT response;
    private final Abortable abortable;

    @SdkInternalApi
    ResponseInputStream(ResponseT resp, AbortableInputStream in) {
        super(in);
        this.response = resp;
        this.abortable = in;
    }

    /**
     * @return The unmarshalled POJO response associated with this content.
     */
    public ResponseT response() {
        return response;
    }

    @Override
    public void abort() {
        abortable.abort();
    }
}
