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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ThrowablesTest {

    @Test
    public void typical() {
        Throwable a = new Throwable();
        Throwable b = new Throwable(a);
        assertSame(a, Throwables.getRootCause(b));
        assertSame(a, Throwables.getRootCause(a));
    }

    @Test
    public void circularRef() {
        // God forbidden
        Throwable a = new Throwable();
        Throwable b = new Throwable(a);
        a.initCause(b);
        assertSame(b, Throwables.getRootCause(b));
        assertSame(a, Throwables.getRootCause(a));
    }

    @Test
    public void nullCause() {
        Throwable a = new Throwable();
        assertSame(a, Throwables.getRootCause(a));
    }

    @Test
    public void simplyNull() {
        assertNull(Throwables.getRootCause(null));
    }
}
