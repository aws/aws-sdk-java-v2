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

package software.amazon.awssdk.core.util;

import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Utility for use with errors or exceptions.
 */
@SdkInternalApi
public final class Throwables {

    private Throwables() {
    }

    /**
     * Returns the root cause of the given throwable, or null if the given
     * throwable is null. If the root cause is over 1000 level deep, the
     * original throwable will be returned defensively as this is heuristically
     * considered a circular reference, however unlikely.
     */
    public static Throwable getRootCause(Throwable orig) {
        if (orig == null) {
            return orig;
        }
        Throwable t = orig;
        // defend against (malicious?) circularity
        for (int i = 0; i < 1000; i++) {
            Throwable cause = t.getCause();
            if (cause == null) {
                return t;
            }
            t = cause;
        }
        // Too bad.  Return the original exception.
        LoggerFactory.getLogger(Throwables.class).debug("Possible circular reference detected on {}: [{}]",
                                                        orig.getClass(),
                                                        orig);
        return orig;
    }

    /**
     * Used to help perform common throw-up with minimal wrapping.
     */
    public static RuntimeException failure(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return t instanceof InterruptedException
               ? new AbortedException(t)
               : new SdkClientException(t);
    }

    /**
     * Same as {@link #failure(Throwable)}, but the given errmsg will be used if
     * it was wrapped as either an {@link SdkClientException} or
     * {@link AbortedException}.
     */
    public static RuntimeException failure(Throwable t, String errmsg) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return t instanceof InterruptedException
               ? new AbortedException(errmsg, t)
               : new SdkClientException(errmsg, t);
    }
}
