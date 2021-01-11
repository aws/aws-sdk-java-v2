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

package software.amazon.awssdk.core.internal.io;

import static software.amazon.awssdk.utils.IoUtils.closeQuietly;

import java.io.Closeable;
import org.slf4j.Logger;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Used for releasing a resource.
 * <p>
 * For example, the creation of a <code>ResettableInputStream</code> would entail
 * physically opening a file. If the opened file is meant to be closed only (in
 * a finally block) by the very same code block that created it, then it is
 * necessary that the release method must not be called while the execution is
 * made in other stack frames.
 *
 * In such case, as other stack frames may inadvertently or indirectly call the
 * close method of the stream, the creator of the stream would need to
 * explicitly disable the accidental closing via
 * <code>ResettableInputStream#disableClose()</code>, so that the release method
 * becomes the only way to truly close the opened file.
 */
@SdkInternalApi
public interface Releasable {
    /**
     * Releases the allocated resource. This method should not be called except
     * by the caller who allocated the resource at the very top of the call
     * stack. This allows, typically, a {@link Closeable} resource to be not
     * unintentionally released owing to the calling of the
     * {@link Closeable#close()} methods by implementation deep down in the call
     * stack.
     * <p>
     * For example, the creation of a <code>ResettableInputStream</code> would entail
     * physically opening a file. If the opened file is meant to be closed only
     * (in a finally block) by the very same code block that created it, then it
     * is necessary that the release method must not be called while the
     * execution is made in other stack frames.
     *
     * In such case, as other stack frames may inadvertently or indirectly call
     * the close method of the stream, the creator of the stream would need to
     * explicitly disable the accidental closing via
     * <code>ResettableInputStream#disableClose()</code>, so that the release method
     * becomes the only way to truly close the opened file.
     */
    void release();

    /**
     * Releases the given {@link Closeable} especially if it was an instance of
     * {@link Releasable}.
     * <p>
     * For example, the creation of a <code>ResettableInputStream</code> would entail
     * physically opening a file. If the opened file is meant to be closed only
     * (in a finally block) by the very same code block that created it, then it
     * is necessary that the release method must not be called while the
     * execution is made in other stack frames.
     *
     * In such case, as other stack frames may inadvertently or indirectly call
     * the close method of the stream, the creator of the stream would need to
     * explicitly disable the accidental closing via
     * <code>ResettableInputStream#disableClose()</code>, so that the release method
     * becomes the only way to truly close the opened file.
     */
    static void release(Closeable is, Logger log) {
        closeQuietly(is, log);
        if (is instanceof Releasable) {
            Releasable r = (Releasable) is;
            r.release();
        }
    }
}
