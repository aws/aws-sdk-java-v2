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

package software.amazon.awssdk.transfer.s3.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;

/**
 * Contains the information of a pausible upload or download; such
 * information can be used to resume the upload or download later on
 *
 * @see FileDownload#pause()
 */
@SdkPublicApi
public interface ResumableTransfer {

    /**
     * Persists this download object to a file in Base64-encoded JSON format.
     *
     * @param path The path to the file to which you want to write the serialized download object.
     */
    default void serializeToFile(Path path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes the serialized JSON data representing this object to an output stream.
     * Note that the {@link OutputStream} is not closed or flushed after writing.
     *
     * @param outputStream The output stream to write the serialized object to.
     */
    default void serializeToOutputStream(OutputStream outputStream) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the serialized JSON data representing this object as a string.
     */
    default String serializeToString() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the serialized JSON data representing this object as an {@link SdkBytes} object.
     *
     * @return the serialized JSON as {@link SdkBytes}
     */
    default SdkBytes serializeToBytes() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the serialized JSON data representing this object as an {@link InputStream}.
     *
     * @return the serialized JSON input stream
     */
    default InputStream serializeToInputStream() {
        throw new UnsupportedOperationException();
    }

}
