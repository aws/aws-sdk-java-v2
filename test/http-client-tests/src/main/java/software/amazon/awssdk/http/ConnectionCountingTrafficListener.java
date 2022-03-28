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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.utils.BinaryUtils;

public class ConnectionCountingTrafficListener implements WiremockNetworkTrafficListener {
    private final Map<Socket, ByteArrayOutputStream> sockets = new ConcurrentHashMap<>();

    @Override
    public void opened(Socket socket) {
        sockets.put(socket, new ByteArrayOutputStream());
    }

    @Override
    public void incoming(Socket socket, ByteBuffer bytes) {
        invokeSafely(() -> sockets.get(socket).write(BinaryUtils.copyBytesFrom(bytes.asReadOnlyBuffer())));
    }

    @Override
    public void outgoing(Socket socket, ByteBuffer bytes) {
    }

    @Override
    public void closed(Socket socket) {
    }

    public int openedConnections() {
        int count = 0;
        for (ByteArrayOutputStream data : sockets.values()) {
            byte[] bytes = data.toByteArray();
            try {
                if (new String(bytes, StandardCharsets.UTF_8).startsWith("POST /__admin/mappings")) {
                    // Admin-related stuff, don't count it
                    continue;
                }

                // Not admin-related stuff, so count it
                ++count;
            } catch (RuntimeException e) {
                // Could not decode, so it's not admin-related stuff (which uses JSON and can always be decoded)
                ++count;
            }
        }
        return count;
    }
}
