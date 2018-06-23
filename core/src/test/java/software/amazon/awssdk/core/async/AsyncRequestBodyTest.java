/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.async.SimpleSubscriber;

@RunWith(Parameterized.class)
public class AsyncRequestBodyTest {
    private final static String testString = "Hello!";
    private final static Path path;

    static {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        path = fs.getPath("./test");
        try {
            Files.write(path, testString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Parameterized.Parameters
    public static AsyncRequestBody[] data() {
        return new AsyncRequestBody[]{
                AsyncRequestBody.fromString(testString),
                AsyncRequestBody.fromFile(path)
        };
    }

    private AsyncRequestBody provider;

    public AsyncRequestBodyTest(AsyncRequestBody provider) {
        this.provider = provider;
    }

    @Test
    public void hasCorrectLength() {
        assertThat(provider.contentLength()).isEqualTo(testString.length());
    }

    @Test
    public void hasCorrectContent() throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);

        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(buffer -> {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            sb.append(new String(bytes, StandardCharsets.UTF_8));
        }) {
            @Override
            public void onError(Throwable t) {
                super.onError(t);
                done.countDown();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                done.countDown();
            }
        };

        provider.subscribe(subscriber);
        done.await();
        assertThat(sb.toString()).isEqualTo(testString);
    }
}