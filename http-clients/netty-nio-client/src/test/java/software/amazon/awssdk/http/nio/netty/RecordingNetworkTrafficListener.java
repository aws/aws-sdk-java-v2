/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty;

import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Simple implementation of {@link WiremockNetworkTrafficListener} to record all requests received as a string for later
 * verification.
 */
public class RecordingNetworkTrafficListener implements WiremockNetworkTrafficListener {
    private final StringBuilder requests = new StringBuilder();


    @Override
    public void opened(Socket socket) {

    }

    @Override
    public void incoming(Socket socket, ByteBuffer byteBuffer) {
        requests.append(StandardCharsets.UTF_8.decode(byteBuffer));
    }

    @Override
    public void outgoing(Socket socket, ByteBuffer byteBuffer) {

    }

    @Override
    public void closed(Socket socket) {

    }

    public void reset() {
        requests.setLength(0);
    }

    public StringBuilder requests() {
        return requests;
    }
}