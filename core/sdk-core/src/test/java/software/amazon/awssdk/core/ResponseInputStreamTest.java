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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;

class ResponseInputStreamTest {
    @Test
    void abort_withAbortable_closesUnderlyingStream() throws IOException {
        InputStream stream = Mockito.mock(InputStream.class);
        Abortable abortable = Mockito.mock(Abortable.class);
        AbortableInputStream abortableInputStream = AbortableInputStream.create(stream, abortable);
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        responseInputStream.abort();

        Mockito.verify(abortable).abort();
        Mockito.verify(stream).close();
    }

    @Test
    void failedClose_withinAbort_isIgnored() throws IOException {
        InputStream stream = Mockito.mock(InputStream.class);
        Abortable abortable = Mockito.mock(Abortable.class);
        AbortableInputStream abortableInputStream = AbortableInputStream.create(stream, abortable);
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        Mockito.doThrow(new IOException()).when(stream).close();
        assertThatCode(responseInputStream::abort).doesNotThrowAnyException();

        Mockito.verify(abortable).abort();
        Mockito.verify(stream).close();
    }

    @Test
    void abort_withoutAbortable_closesUnderlyingStream() throws IOException {
        InputStream stream = Mockito.mock(InputStream.class);
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), stream);

        responseInputStream.abort();

        Mockito.verify(stream).close();
    }

    @Test
    void close_withAbortable_closesUnderlyingStream() throws IOException {
        InputStream stream = Mockito.mock(InputStream.class);
        Abortable abortable = Mockito.mock(Abortable.class);
        AbortableInputStream abortableInputStream = AbortableInputStream.create(stream, abortable);
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        responseInputStream.close();

        Mockito.verify(abortable, never()).abort();
        Mockito.verify(stream).close();
    }
}