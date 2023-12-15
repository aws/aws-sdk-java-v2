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

package software.amazon.awssdk.core.internal.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;

public class BytesReadTrackingInputStreamTest {
    private InputStream mockStream = mock(InputStream.class);
    private AbortableInputStream abortableStream;

    @BeforeEach
    private void setup() {
        reset(mockStream);
        abortableStream = AbortableInputStream.create(mockStream);
    }

    @Test
    public void readByte_returnsEof_doesNotUpdateTotal() throws IOException {
        when(mockStream.read()).thenReturn(-1);
        BytesReadTrackingInputStream trackingInputStream = newTrackingStream();

        trackingInputStream.read();

        verify(mockStream).read();
        verifyNoMoreInteractions(mockStream);

        assertThat(trackingInputStream.bytesRead()).isEqualTo(0);
    }

    @Test
    public void readByteArray_returnsEof_doesNotUpdateTotal() throws IOException {
        when(mockStream.read(any(byte[].class))).thenReturn(-1);

        BytesReadTrackingInputStream trackingInputStream = newTrackingStream();

        trackingInputStream.read(new byte[8]);

        verify(mockStream).read(any(byte[].class), eq(0), eq(8));
        verifyNoMoreInteractions(mockStream);

        assertThat(trackingInputStream.bytesRead()).isEqualTo(0);
    }

    @Test
    public void readByteArrayRange_returnsEof_doesNotUpdateTotal() throws IOException {
        when(mockStream.read(any(byte[].class))).thenReturn(-1);

        BytesReadTrackingInputStream trackingInputStream = newTrackingStream();

        trackingInputStream.read(new byte[8], 2, 4);

        verify(mockStream).read(any(byte[].class), eq(2), eq(4));
        verifyNoMoreInteractions(mockStream);

        assertThat(trackingInputStream.bytesRead()).isEqualTo(0);
    }

    @Test
    public void skip_returnedSkipEof_doesNotUpdateTotal() throws IOException {
        when(mockStream.skip(eq(8L))).thenReturn(-1L);
        BytesReadTrackingInputStream trackingInputStream = newTrackingStream();

        trackingInputStream.skip(8);

        verify(mockStream).skip(eq(8L));
        verifyNoMoreInteractions(mockStream);

        assertThat(trackingInputStream.bytesRead()).isEqualTo(0);
    }

    @Test
    public void skip_returnedSkipReturnsPositive_updatesTotal() throws IOException {
        when(mockStream.skip(eq(8L))).thenReturn(8L);
        BytesReadTrackingInputStream trackingInputStream = newTrackingStream();

        trackingInputStream.skip(8);

        verify(mockStream).skip(eq(8L));
        verifyNoMoreInteractions(mockStream);

        assertThat(trackingInputStream.bytesRead()).isEqualTo(8);
    }

    @Test
    public void readByte_returnsPositive_updatesTotal() throws IOException {
        when(mockStream.read()).thenReturn(8);
        BytesReadTrackingInputStream trackingInputStream = newTrackingStream();

        trackingInputStream.read();

        verify(mockStream).read();
        verifyNoMoreInteractions(mockStream);

        assertThat(trackingInputStream.bytesRead()).isEqualTo(8);
    }

    @Test
    public void readByteArray_returnsPositive_updatesTotal() throws IOException {
        when(mockStream.read(any(byte[].class), eq(0), eq(8))).thenReturn(4);

        BytesReadTrackingInputStream trackingInputStream = newTrackingStream();

        trackingInputStream.read(new byte[8]);

        verify(mockStream).read(any(byte[].class), eq(0), eq(8));
        verifyNoMoreInteractions(mockStream);

        assertThat(trackingInputStream.bytesRead()).isEqualTo(8);
    }

    @Test
    public void readByteArrayRange_returnsPositive_updatesTotal() throws IOException {
        when(mockStream.read(any(byte[].class), eq(2), eq(4))).thenReturn(4);

        BytesReadTrackingInputStream trackingInputStream = newTrackingStream();

        trackingInputStream.read(new byte[8], 2, 4);

        verify(mockStream).read(any(byte[].class), eq(2), eq(4));
        verifyNoMoreInteractions(mockStream);

        assertThat(trackingInputStream.bytesRead()).isEqualTo(4);
    }

    @Test
    public void abort_abortsDelegate() {
        Abortable mockAbortable = mock(Abortable.class);
        AbortableInputStream abortableIs = AbortableInputStream.create(mockStream, mockAbortable);
        BytesReadTrackingInputStream trackingInputStream = new BytesReadTrackingInputStream(abortableIs, new AtomicLong(0));
        trackingInputStream.abort();

        verify(mockAbortable).abort();
    }

    private BytesReadTrackingInputStream newTrackingStream(AtomicLong read) {
        return new BytesReadTrackingInputStream(abortableStream, read);
    }

    private BytesReadTrackingInputStream newTrackingStream() {
        return newTrackingStream(new AtomicLong(0));
    }
}
