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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.io.FilterInputStream;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Input stream that can be aborted. Abort typically means to destroy underlying HTTP connection
 * without reading more data. This may be desirable when the cost of reading the rest of the data
 * exceeds that of establishing a new connection.
 */
@SdkProtectedApi
public final class AbortableInputStream extends FilterInputStream implements Abortable {

    private final Abortable abortable;

    private AbortableInputStream(InputStream delegate, Abortable abortable) {
        super(paramNotNull(delegate, "delegate"));
        this.abortable = paramNotNull(abortable, "abortable");
    }

    /**
     * Creates an instance of {@link AbortableInputStream}.
     *
     * @param delegate the delegated input stream
     * @param abortable the abortable
     * @return a new instance of AbortableInputStream
     */
    public static AbortableInputStream create(InputStream delegate, Abortable abortable) {
        return new AbortableInputStream(delegate, abortable);
    }

    /**
     * Creates an instance of {@link AbortableInputStream} that ignores abort.
     *
     * @param delegate the delegated input stream
     * @return a new instance of AbortableInputStream
     */
    public static AbortableInputStream create(InputStream delegate) {
        if (delegate instanceof Abortable) {
            return new AbortableInputStream(delegate, (Abortable) delegate);
        }
        return new AbortableInputStream(delegate, () -> { });
    }

    @Override
    public void abort() {
        abortable.abort();
    }

}
