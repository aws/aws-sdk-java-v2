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

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.io.SdkFilterInputStream;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Input stream that provides access to the unmarshalled POJO response returned by the service in addition to the streamed
 * contents. This input stream should be closed after all data has been read from the stream.
 *
 * <p>
 * <b>NOTE:</b> You must read this stream promptly to avoid automatic cancellation. The default timeout for reading is 60
 * seconds. If {@link #read()} is not invoked before the timeout, the stream will automatically abort to prevent resource leakage.
 * <p>
 * The timeout can be customized by passing a {@link Duration} to the constructor, or disabled entirely by
 * passing {@link Duration#ZERO}.
 * <p>
 * Note about the Apache http client: This input stream can be used to leverage a feature of the Apache http client where
 * connections are released back to the connection pool to be reused. As such, calling {@link ResponseInputStream#close() close}
 * on this input stream will result in reading the remaining data from the stream and leaving the connection open, even if the
 * stream was only partially read from. For large http payload, this means reading <em>all</em> of the http body before releasing
 * the connection which may add latency.
 * <p>
 * If it is not desired to read remaining data from the stream, you can explicitly abort the connection via {@link #abort()}
 * instead. This will close the underlying connection and require establishing a new HTTP connection on subsequent requests which
 * may outweigh the cost of reading the additional data.
 * <p>
 * The Url Connection and Crt http clients are not subject to this behaviour so the {@link ResponseInputStream#close() close} and
 * {@link ResponseInputStream#abort() abort} methods will behave similarly with them.
 */
@SdkPublicApi
public final class ResponseInputStream<ResponseT> extends SdkFilterInputStream implements Abortable {

    private static final Logger log = Logger.loggerFor(ResponseInputStream.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private final ResponseT response;
    private final Abortable abortable;
    private ScheduledFuture<?> timeoutTask;
    private volatile boolean hasRead = false;

    public ResponseInputStream(ResponseT resp, AbortableInputStream in) {
        this(resp, in, null);
    }

    public ResponseInputStream(ResponseT resp, AbortableInputStream in, Duration timeout) {
        super(in);
        this.response = Validate.paramNotNull(resp, "response");
        this.abortable = Validate.paramNotNull(in, "abortableInputStream");
        
        Duration resolvedTimeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
        scheduleTimeoutTask(resolvedTimeout);
    }

    public ResponseInputStream(ResponseT resp, InputStream in) {
        super(in);
        this.response = Validate.paramNotNull(resp, "response");
        this.abortable = in instanceof Abortable ? (Abortable) in : null;
        scheduleTimeoutTask(DEFAULT_TIMEOUT);
    }

    /**
     * @return The unmarshalled POJO response associated with this content.
     */
    public ResponseT response() {
        return response;
    }

    @Override
    public int read() throws IOException {
        cancelTimeoutTask();
        return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        cancelTimeoutTask();
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        cancelTimeoutTask();
        return super.read(b, off, len);
    }

    private void cancelTimeoutTask() {
        hasRead = true;
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }
    }

    private void scheduleTimeoutTask(Duration timeout) {
        if (timeout.equals(Duration.ZERO)) {
            return;
        }

        long timeoutInMillis = timeout.toMillis();
        timeoutTask = TimeoutScheduler.INSTANCE.schedule(() -> {
            if (!hasRead) {
                log.debug(() -> String.format("InputStream was not read before timeout of [%d] milliseconds, aborting "
                                              + "stream and closing connection.", timeoutInMillis));
                abort();
            }
        }, timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    private static final class TimeoutScheduler {
        static final ScheduledExecutorService INSTANCE =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "response-input-stream-timeout-scheduler");
                t.setDaemon(true);
                return t;
            });
    }

    /**
     * Close the underlying connection, dropping all remaining data in the stream, and not leaving the
     * connection open to be used for future requests.
     */
    @Override
    public void abort() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }
        if (abortable != null) {
            abortable.abort();
        }
        IoUtils.closeQuietlyV2(in, log);
    }

    @SdkTestInternalApi
    public boolean hasTimeoutTask() {
        return timeoutTask != null;
    }

    @SdkTestInternalApi
    public boolean timeoutTaskDoneOrCancelled() {
        return timeoutTask != null && timeoutTask.isDone();
    }
}
