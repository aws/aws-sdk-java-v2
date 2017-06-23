/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.async;

import java.nio.ByteBuffer;

/**
 * A object that allows writing data to an open socket.
 */
public interface SdkRequestChannel {

    /**
     * Write data to the socket.
     * <br/>
     * This operation should not block on IO. Data should be buffered until it can be written
     * to the socket. This method should only block if the buffer is full and back-pressure
     * needs to be applied.
     *
     * @param data - the data to write
     */
    void write(ByteBuffer data);

    /**
     * Signify that all data has been written and this request is complete.
     */
    void complete();

    /**
     * Signify that this request should be aborted.
     */
    void abort();
}
