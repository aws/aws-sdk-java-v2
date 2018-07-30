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

package software.amazon.awssdk.core.io;

import java.io.IOException;
import java.io.InputStream;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.internal.io.Releasable;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Base class for AWS Java SDK specific {@link InputStream}.
 */
@SdkProtectedApi
public abstract class SdkInputStream extends InputStream implements Releasable {
    /**
     * Returns the underlying input stream, if any, from the subclass; or null
     * if there is no underlying input stream.
     */
    protected abstract InputStream getWrappedInputStream();

    /**
     * Aborts with subclass specific abortion logic executed if needed.
     * Note the interrupted status of the thread is cleared by this method.
     * @throws AbortedException if found necessary.
     */
    protected final void abortIfNeeded() {
        if (Thread.currentThread().isInterrupted()) {
            try {
                abort();    // execute subclass specific abortion logic
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).debug("FYI", e);
            }
            throw AbortedException.builder().build();
        }
    }

    /**
     * Can be used to provide abortion logic prior to throwing the
     * AbortedException. No-op by default.
     */
    protected void abort() throws IOException {
        // no-op by default, but subclass such as S3ObjectInputStream may override
    }

    /**
     * WARNING: Subclass that overrides this method must NOT call
     * super.release() or else it would lead to infinite loop.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void release() {
        // Don't call IOUtils.release(in, null) or else could lead to infinite loop
        IoUtils.closeQuietly(this, null);
        InputStream in = getWrappedInputStream();
        if (in instanceof Releasable) {
            // This allows any underlying stream that has the close operation
            // disabled to be truly released
            Releasable r = (Releasable) in;
            r.release();
        }
    }
}
