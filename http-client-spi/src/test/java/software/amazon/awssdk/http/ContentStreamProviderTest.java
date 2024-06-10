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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

class ContentStreamProviderTest {
    @Test
    void fromMethods_failOnNull() {
        assertThatThrownBy(() -> ContentStreamProvider.fromByteArray(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ContentStreamProvider.fromByteArrayUnsafe(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ContentStreamProvider.fromString("foo", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ContentStreamProvider.fromString(null, StandardCharsets.UTF_8)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ContentStreamProvider.fromUtf8String(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ContentStreamProvider.fromInputStream(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ContentStreamProvider.fromInputStreamSupplier(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromByteArray_containsInputBytes() throws IOException {
        byte[] bytes = "foo".getBytes();
        ContentStreamProvider provider = ContentStreamProvider.fromByteArray(bytes);
        assertArrayEquals(bytes, IoUtils.toByteArray(provider.newStream()));
        assertArrayEquals(bytes, IoUtils.toByteArray(provider.newStream()));
    }

    @Test
    void fromByteArray_doesNotAllowModifyingInputBytes() throws IOException {
        byte[] bytes = "foo".getBytes();
        byte[] bytesCopy = Arrays.copyOf(bytes, bytes.length);

        ContentStreamProvider provider = ContentStreamProvider.fromByteArray(bytes);
        assertArrayEquals(bytes, IoUtils.toByteArray(provider.newStream()));
        bytes[0] = 'b';
        assertArrayEquals(bytesCopy, IoUtils.toByteArray(provider.newStream()));
    }

    @Test
    void fromByteArrayUnsafe_containsInputBytes() throws IOException {
        byte[] bytes = "foo".getBytes();
        ContentStreamProvider provider = ContentStreamProvider.fromByteArray(bytes);
        assertArrayEquals(bytes, IoUtils.toByteArray(provider.newStream()));
        assertArrayEquals(bytes, IoUtils.toByteArray(provider.newStream()));
    }

    @Test
    void fromByteArrayUnsafe_doesNotProtectAgainstModifyingInputBytes() throws IOException {
        byte[] bytes = "foo".getBytes();

        ContentStreamProvider provider = ContentStreamProvider.fromByteArrayUnsafe(bytes);
        assertArrayEquals(bytes, IoUtils.toByteArray(provider.newStream()));
        bytes[0] = 'b';
        assertArrayEquals(bytes, IoUtils.toByteArray(provider.newStream()));
    }

    @Test
    void fromString_containsInputBytes() throws IOException {
        String str = "foo";
        ContentStreamProvider provider = ContentStreamProvider.fromString(str, StandardCharsets.UTF_8);
        assertEquals(str, IoUtils.toUtf8String(provider.newStream()));
        assertEquals(str, IoUtils.toUtf8String(provider.newStream()));
    }

    @Test
    void fromString_honorsEncoding() throws IOException {
        String str = "\uD83D\uDE0A";
        ContentStreamProvider asciiProvider = ContentStreamProvider.fromString(str, StandardCharsets.US_ASCII);
        ContentStreamProvider utf8Provider = ContentStreamProvider.fromString(str, StandardCharsets.UTF_8);
        assertArrayEquals("?".getBytes(StandardCharsets.US_ASCII), IoUtils.toByteArray(asciiProvider.newStream()));
        assertArrayEquals("\uD83D\uDE0A".getBytes(StandardCharsets.UTF_8), IoUtils.toByteArray(utf8Provider.newStream()));
    }

    @Test
    void fromUtf8String_containsInputBytes() throws IOException {
        String str = "foo";
        ContentStreamProvider provider = ContentStreamProvider.fromUtf8String(str);
        assertEquals(str, IoUtils.toUtf8String(provider.newStream()));
        assertEquals(str, IoUtils.toUtf8String(provider.newStream()));
    }

    @Test
    void fromInputStream_containsInputBytes() throws IOException {
        String str = "foo";
        ContentStreamProvider provider = ContentStreamProvider.fromInputStream(new StringInputStream(str));
        assertEquals(str, IoUtils.toUtf8String(provider.newStream()));
        assertEquals(str, IoUtils.toUtf8String(provider.newStream()));
    }

    @Test
    void fromInputStream_failsOnSecondNewStream_ifInputStreamDoesNotSupportMarkAndReset() {
        InputStream stream = new InputStream() {
            @Override
            public int read() {
                return 0;
            }
        };

        ContentStreamProvider provider = ContentStreamProvider.fromInputStream(stream);
        provider.newStream();
        assertThatThrownBy(provider::newStream).isInstanceOf(IllegalStateException.class)
                                               .hasMessageContaining("reset");
    }

    @Test
    void fromInputStream_marksStream() throws IOException {
        InputStream stream = Mockito.mock(InputStream.class);
        Mockito.when(stream.markSupported()).thenReturn(true);

        ContentStreamProvider provider = ContentStreamProvider.fromInputStream(stream);
        provider.newStream().read();
        Mockito.verify(stream, Mockito.atLeastOnce()).mark(Mockito.anyInt());
    }

    @Test
    void fromInputStream_doesNotCloseProvidedResettableStream() throws IOException {
        InputStream stream = Mockito.mock(InputStream.class);
        Mockito.when(stream.markSupported()).thenReturn(true);

        ContentStreamProvider provider = ContentStreamProvider.fromInputStream(stream);
        provider.newStream().read();
        provider.newStream().read();
        Mockito.verify(stream, Mockito.never()).close();
    }

    @Test
    void fromInputStream_doesNotCloseProvidedSingleUseStream() throws IOException {
        InputStream stream = Mockito.mock(InputStream.class);
        Mockito.when(stream.markSupported()).thenReturn(false);

        ContentStreamProvider provider = ContentStreamProvider.fromInputStream(stream);
        provider.newStream().read();
        Mockito.verify(stream, Mockito.never()).close();
    }

    @Test
    void fromInputStreamSupplier_containsInputBytes() throws IOException {
        String str = "foo";
        ContentStreamProvider provider = ContentStreamProvider.fromInputStreamSupplier(() -> new StringInputStream(str));
        assertEquals(str, IoUtils.toUtf8String(provider.newStream()));
        assertEquals(str, IoUtils.toUtf8String(provider.newStream()));
    }

    @Test
    void fromInputStreamSupplier_closesStreams() throws IOException {
        InputStream stream1 = Mockito.mock(InputStream.class);
        InputStream stream2 = Mockito.mock(InputStream.class);
        Supplier<InputStream> streamSupplier = Mockito.mock(Supplier.class);

        Mockito.when(streamSupplier.get()).thenReturn(stream1, stream2);

        ContentStreamProvider provider = ContentStreamProvider.fromInputStreamSupplier(streamSupplier);

        provider.newStream();

        Mockito.verify(stream1, Mockito.never()).close();
        Mockito.verify(stream2, Mockito.never()).close();

        provider.newStream();

        Mockito.verify(stream1, Mockito.times(1)).close();
        Mockito.verify(stream2, Mockito.never()).close();
    }
}