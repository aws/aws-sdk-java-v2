/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.internal.cloudwatch;

import java.util.concurrent.BlockingQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.util.VersionInfoUtils;

/**
 * A internal daemon thread used to upload request metrics to Amazon CloudWatch.
 */
class MetricUploaderThread extends Thread {
    private static final String USER_AGENT = MetricUploaderThread.class.getName() + "/" + VersionInfoUtils.getVersion();
    private static final String THREAD_NAME = "java-sdk-metric-uploader";
    private final CloudWatchClient cloudwatchClient;
    private final Log log = LogFactory.getLog(getClass());
    private final BlockingRequestBuilder qIterator;
    private volatile boolean cancelled;

    MetricUploaderThread(CloudWatchMetricConfig config,
                         BlockingQueue<MetricDatum> queue) {
        super(THREAD_NAME);
        if (config == null || queue == null) {
            throw new IllegalArgumentException();
        }
        this.cloudwatchClient = config.getCloudWatchClient();
        this.qIterator = new BlockingRequestBuilder(config, queue);
        this.setPriority(MIN_PRIORITY);
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!cancelled) {
            try {
                Iterable<PutMetricDataRequest> requests = qIterator.nextUploadUnits();
                for (PutMetricDataRequest req : requests) {
                    appendUserAgent(req);
                    log.debug(req);
                    cloudwatchClient.putMetricData(req);
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                if (!cancelled) {
                    log.debug("Unexpected interruption ignored");
                }
            } catch (Throwable t) {
                log.warn("Unexpected condition; soldier on", t);
                Thread.yield();
            }
        }
    }

    void cancel() {
        cancelled = true;
    }

    private void appendUserAgent(PutMetricDataRequest request) {
        request.getRequestClientOptions().appendUserAgent(USER_AGENT);
    }

}
