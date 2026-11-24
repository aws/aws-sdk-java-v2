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

package software.amazon.awssdk.auth.credentials.internal.daemon;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Opens Unix domain socket channels and creates their addresses, with either the JDK (Java 16 or later) or junixsocket.
 */
@SdkInternalApi
interface UnixDomainSockets {
    /**
     * Opens a blocking Unix domain socket channel.
     */
    SocketChannel openChannel() throws IOException;

    /**
     * Returns the Unix domain socket address of the path.
     *
     * @throws IOException or {@link java.nio.file.InvalidPathException} if the path cannot be used as a socket address.
     */
    SocketAddress address(Path path) throws IOException;
}
