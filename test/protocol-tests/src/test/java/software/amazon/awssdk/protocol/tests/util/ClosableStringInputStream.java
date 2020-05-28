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

package software.amazon.awssdk.protocol.tests.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Closable StringInputStream. Once closed, cannot be read again
 */
public class ClosableStringInputStream extends ByteArrayInputStream {
    private final String string;
    private boolean isClosed;

    public ClosableStringInputStream(String s) {
        super(s.getBytes(StandardCharsets.UTF_8));
        this.string = s;
    }

    @Override
    public void close() {
        isClosed = true;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public String getString() {
        return string;
    }
}
