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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class BytesWrittenTrackingInputStreamTest {

    @Test
    public void readSingleByte_updatesCounter() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{1, 2, 3});
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        stream.read();
        assertThat(counter.get()).isEqualTo(1);

        stream.read();
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    public void readSingleByte_eof_doesNotUpdateCounter() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[0]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        int result = stream.read();

        assertThat(result).isEqualTo(-1);
        assertThat(counter.get()).isEqualTo(0);
    }

    @Test
    public void readByteArray_updatesCounter() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[10]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        int bytesRead = stream.read(new byte[5]);

        assertThat(bytesRead).isEqualTo(5);
        assertThat(counter.get()).isEqualTo(5);
    }

    @Test
    public void readByteArray_eof_doesNotUpdateCounter() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[0]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        int result = stream.read(new byte[5]);

        assertThat(result).isEqualTo(-1);
        assertThat(counter.get()).isEqualTo(0);
    }

    @Test
    public void readByteArrayWithOffset_updatesCounter() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[10]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        int bytesRead = stream.read(new byte[10], 2, 5);

        assertThat(bytesRead).isEqualTo(5);
        assertThat(counter.get()).isEqualTo(5);
    }

    @Test
    public void skip_updatesCounter() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[10]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        long skipped = stream.skip(5);

        assertThat(skipped).isEqualTo(5);
        assertThat(counter.get()).isEqualTo(5);
    }

    @Test
    public void skip_zeroBytes_doesNotUpdateCounter() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[0]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        long skipped = stream.skip(5);

        assertThat(skipped).isEqualTo(0);
        assertThat(counter.get()).isEqualTo(0);
    }

    @Test
    public void multipleReads_accumulatesCounter() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[100]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        stream.read();
        stream.read(new byte[10]);
        stream.read(new byte[20], 0, 15);
        stream.skip(5);

        assertThat(counter.get()).isEqualTo(1 + 10 + 15 + 5);
    }

    @Test
    public void firstRead_recordsStartTime() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[10]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        assertThat(startTime.get()).isEqualTo(0);

        stream.read();

        assertThat(startTime.get()).isGreaterThan(0);
    }

    @Test
    public void subsequentReads_doNotChangeStartTime() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[10]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        stream.read();
        long firstStartTime = startTime.get();

        stream.read();
        stream.read(new byte[5]);

        assertThat(startTime.get()).isEqualTo(firstStartTime);
    }

    @Test
    public void lastReadTime_updatesOnEveryRead() throws IOException {
        AtomicLong counter = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong lastReadTime = new AtomicLong(0);
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[10]);
        BytesWrittenTrackingInputStream stream = new BytesWrittenTrackingInputStream(source, counter, startTime, lastReadTime);

        stream.read();
        long firstLastReadTime = lastReadTime.get();
        assertThat(firstLastReadTime).isGreaterThan(0);

        stream.read(new byte[5]);
        assertThat(lastReadTime.get()).isGreaterThanOrEqualTo(firstLastReadTime);
    }
}
