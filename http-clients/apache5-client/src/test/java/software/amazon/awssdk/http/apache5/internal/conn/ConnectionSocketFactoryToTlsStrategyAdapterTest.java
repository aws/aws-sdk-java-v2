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

package software.amazon.awssdk.http.apache5.internal.conn;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.stream.Stream;

class ConnectionSocketFactoryToTlsStrategyAdapterTest {

    @Mock
    private Socket mockSocket;

    @Mock
    private SSLSocket mockSslSocket;

    @Mock
    private HttpContext mockContext;

    @Mock
    private LayeredConnectionSocketFactory mockLayeredFactory;

    @Mock
    private ConnectionSocketFactory mockPlainFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void upgrade_withLayeredConnectionSocketFactory_returnsSSLSocket() throws IOException {
        
        when(mockLayeredFactory.createLayeredSocket(mockSocket, "example.com", 443, mockContext))
            .thenReturn(mockSslSocket);

        ConnectionSocketFactoryToTlsStrategyAdapter adapter =
            new ConnectionSocketFactoryToTlsStrategyAdapter(mockLayeredFactory);

        
        SSLSocket result = adapter.upgrade(mockSocket, "example.com", 443, null, mockContext);

        
        assertThat(result).isEqualTo(mockSslSocket);
        verify(mockLayeredFactory).createLayeredSocket(mockSocket, "example.com", 443, mockContext);
    }

    @Test
    void upgrade_withPlainConnectionSocketFactory_returnsNull() throws IOException {
        
        ConnectionSocketFactoryToTlsStrategyAdapter adapter =
            new ConnectionSocketFactoryToTlsStrategyAdapter(PlainConnectionSocketFactory.INSTANCE);

        
        SSLSocket result = adapter.upgrade(mockSocket, "example.com", 443, null, mockContext);

        
        assertThat(result).isNull();
    }

    @Test
    void upgrade_whenLayeredFactoryReturnsNull_throwsIOException() throws IOException {
        
        when(mockLayeredFactory.createLayeredSocket(any(), any(), anyInt(), any()))
            .thenReturn(null);

        ConnectionSocketFactoryToTlsStrategyAdapter adapter =
            new ConnectionSocketFactoryToTlsStrategyAdapter(mockLayeredFactory);

        
        assertThatThrownBy(() ->
                               adapter.upgrade(mockSocket, "example.com", 443, null, mockContext))
            .isInstanceOf(IOException.class)
            .hasMessage("LayeredConnectionSocketFactory.createLayeredSocket returned null");
    }

    @Test
    void upgrade_whenLayeredFactoryReturnsNonSSLSocket_throwsIOException() throws IOException {
        
        Socket nonSslSocket = mock(Socket.class);
        when(mockLayeredFactory.createLayeredSocket(any(), any(), anyInt(), any()))
            .thenReturn(nonSslSocket);

        ConnectionSocketFactoryToTlsStrategyAdapter adapter =
            new ConnectionSocketFactoryToTlsStrategyAdapter(mockLayeredFactory);

        
        assertThatThrownBy(() ->
                               adapter.upgrade(mockSocket, "example.com", 443, null, mockContext))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("did not return an SSLSocket")
            .hasMessageContaining(nonSslSocket.getClass().getName());
    }

    @ParameterizedTest
    @MethodSource("provideFactoriesAndExpectedResults")
    void upgrade_withDifferentFactoryTypes_behavesCorrectly(
        ConnectionSocketFactory factory,
        boolean shouldReturnNull,
        String testDescription) throws IOException {
        
        if (factory instanceof LayeredConnectionSocketFactory && !shouldReturnNull) {
            when(((LayeredConnectionSocketFactory) factory).createLayeredSocket(any(), any(), anyInt(), any()))
                .thenReturn(mockSslSocket);
        }

        ConnectionSocketFactoryToTlsStrategyAdapter adapter =
            new ConnectionSocketFactoryToTlsStrategyAdapter(factory);

        
        SSLSocket result = adapter.upgrade(mockSocket, "example.com", 443, null, mockContext);

        
        if (shouldReturnNull) {
            assertThat(result).as(testDescription).isNull();
        } else {
            assertThat(result).as(testDescription).isNotNull();
        }
    }

    private static Stream<Arguments> provideFactoriesAndExpectedResults() {
        return Stream.of(
            Arguments.of(
                PlainConnectionSocketFactory.INSTANCE,
                true,
                "PlainConnectionSocketFactory should return null"
            ),
            Arguments.of(
                mock(ConnectionSocketFactory.class),
                true,
                "Non-layered ConnectionSocketFactory should return null"
            ),
            Arguments.of(
                mock(LayeredConnectionSocketFactory.class),
                false,
                "LayeredConnectionSocketFactory should attempt upgrade"
            )
        );
    }

    @Test
    void upgrade_withLayeredFactory_propagatesIOException() throws IOException {
        
        IOException expectedException = new IOException("Connection failed");
        when(mockLayeredFactory.createLayeredSocket(any(), any(), anyInt(), any()))
            .thenThrow(expectedException);

        ConnectionSocketFactoryToTlsStrategyAdapter adapter =
            new ConnectionSocketFactoryToTlsStrategyAdapter(mockLayeredFactory);

        
        assertThatThrownBy(() ->
                               adapter.upgrade(mockSocket, "example.com", 443, null, mockContext))
            .isEqualTo(expectedException);
    }
}
