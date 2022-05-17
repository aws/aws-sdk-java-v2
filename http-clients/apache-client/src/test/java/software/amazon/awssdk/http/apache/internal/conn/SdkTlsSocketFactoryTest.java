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

package software.amazon.awssdk.http.apache.internal.conn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SdkTlsSocketFactoryTest {

    SdkTlsSocketFactory factory;
    SSLSocket socket;

    @BeforeEach
    public void before() throws Exception {
        factory = new SdkTlsSocketFactory(SSLContext.getDefault(), null);
        socket = Mockito.mock(SSLSocket.class);
    }

    @Test
    void nullProtocols() {
        when(socket.getSupportedProtocols()).thenReturn(null);
        when(socket.getEnabledProtocols()).thenReturn(null);

        factory.prepareSocket(socket);

        verify(socket, never()).setEnabledProtocols(any());
    }

    @Test
    void amazonCorretto_8_0_292_defaultEnabledProtocols() {
        when(socket.getSupportedProtocols()).thenReturn(new String[] {
            "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3", "SSLv2Hello"
        });
        when(socket.getEnabledProtocols()).thenReturn(new String[] {
            "TLSv1.2", "TLSv1.1", "TLSv1"
        });

        factory.prepareSocket(socket);

        verify(socket, never()).setEnabledProtocols(any());
    }

    @Test
    void amazonCorretto_11_0_08_defaultEnabledProtocols() {
        when(socket.getSupportedProtocols()).thenReturn(new String[] {
            "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3", "SSLv2Hello"
        });
        when(socket.getEnabledProtocols()).thenReturn(new String[] {
            "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"
        });

        factory.prepareSocket(socket);

        verify(socket, never()).setEnabledProtocols(any());
    }

    @Test
    void amazonCorretto_17_0_1_defaultEnabledProtocols() {
        when(socket.getSupportedProtocols()).thenReturn(new String[] {
            "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3", "SSLv2Hello"
        });
        when(socket.getEnabledProtocols()).thenReturn(new String[] {
            "TLSv1.3", "TLSv1.2"
        });

        factory.prepareSocket(socket);

        verify(socket, never()).setEnabledProtocols(any());
    }
}
