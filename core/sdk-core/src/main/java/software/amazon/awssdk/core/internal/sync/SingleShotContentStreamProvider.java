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

package software.amazon.awssdk.core.internal.sync;

import java.io.InputStream;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * A {@code ContentStreamProvider} that can only provide the stream once. It throws a {@link NonRetryableException} for all
 * subsequent calls to {@link #newStream()}.
 */
public final class SingleShotContentStreamProvider implements ContentStreamProvider {
    private final InputStream is;
    private boolean available = true;

    public SingleShotContentStreamProvider(InputStream is) {
        this.is = is;
    }

    @Override
    public InputStream newStream() {
        if (available) {
            available = false;
            return is;
        }
        throw NonRetryableException.create("Stream cannot be reset to the initial position.");
    }
}
