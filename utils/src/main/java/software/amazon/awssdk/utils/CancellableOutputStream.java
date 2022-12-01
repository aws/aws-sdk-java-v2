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

package software.amazon.awssdk.utils;

import java.io.OutputStream;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * An implementation of {@link OutputStream} to which writing can be {@link #cancel()}ed.
 * <p>
 * Cancelling tells the downstream receiver of the output that the stream will not be written to anymore, and that the
 * data sent was incomplete. The stream must still be {@link #close()}d by the caller.
 */
@SdkPublicApi
public abstract class CancellableOutputStream extends OutputStream {
    /**
     * Cancel writing to the stream. This is different than {@link #close()} in that it indicates the data written so
     * far is truncated and incomplete. Callers must still invoke {@link #close()} even if the stream is
     * cancelled.
     */
    public abstract void cancel();
}
