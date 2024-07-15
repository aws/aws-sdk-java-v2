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

package software.amazon.awssdk.core.internal.util;

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.metrics.BytesReadTrackingInputStream;
import software.amazon.awssdk.core.internal.metrics.BytesReadTrackingPublisher;
import software.amazon.awssdk.core.internal.progress.listener.ProgressUpdater;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class ProgressListenerUtils {

    private ProgressListenerUtils() {
    }

    public static SdkHttpContentPublisher wrapRequestProviderWithByteTrackingIfProgressListenerAttached(
        SdkHttpContentPublisher requestProvider, RequestExecutionContext context) {
        ProgressUpdater progressUpdater = getProgressUpdaterIfAttached(context);
        SdkHttpContentPublisher wrappedHttpContentPublisher = requestProvider;
        if (progressUpdater != null) {
            wrappedHttpContentPublisher = new BytesReadTrackingPublisher(requestProvider, new AtomicLong(0L),
                                                                         new UploadProgressUpdaterInvocation(progressUpdater));
        }
        return wrappedHttpContentPublisher;
    }

    public static ContentStreamProvider wrapContentStreamProviderWithBytePublishTrackingIfProgressListenerAttached(
        ContentStreamProvider contentStreamProvider, RequestExecutionContext context) {

        ProgressUpdater progressUpdater = getProgressUpdaterIfAttached(context);
        ContentStreamProvider wrappedContentStreamProvider = contentStreamProvider;
        if (progressUpdater != null) {
            wrappedContentStreamProvider =
                ContentStreamProvider.fromInputStream(new BytesReadTrackingInputStream(
                    (AbortableInputStream) contentStreamProvider.newStream(),
                    new AtomicLong(0L),
                    new UploadProgressUpdaterInvocation(progressUpdater)));
        }
        return wrappedContentStreamProvider;
    }

    public static BytesReadTrackingInputStream wrapContentStreamProviderWithByteReadTrackingIfProgressListenerAttached(
        AbortableInputStream content, AtomicLong bytesRead, RequestExecutionContext context) {
        ProgressUpdater progressUpdater = getProgressUpdaterIfAttached(context);

        return new BytesReadTrackingInputStream(content, bytesRead,
                                                new DownloadProgressUpdaterInvocation(progressUpdater));
    }

    public static void updateProgressListenersWithResponseStatus(ProgressUpdater progressUpdater,
                                                                 SdkHttpResponse headers) {
        progressUpdater.responseHeaderReceived();
        headers.firstMatchingHeader(CONTENT_LENGTH).ifPresent(value -> {
            if (!StringUtils.isNotBlank(value)) {
                progressUpdater.updateResponseContentLength(Long.parseLong(value));
            }
        });
    }

    public static void updateProgressListenersWithSuccessResponse(SdkResponse response,
                                                                  RequestExecutionContext context) {

        context.progressUpdater().ifPresent(progressUpdater -> progressUpdater.executionSuccess(response));
    }

    public static boolean progressListenerAttached(SdkRequest request) {
        return request.overrideConfiguration()
                      .map(RequestOverrideConfiguration::progressListeners).isPresent();
    }

    public static ProgressUpdater getProgressUpdaterIfAttached(RequestExecutionContext context) {
        if (context.progressUpdater().isPresent()) {
            return context.progressUpdater().get();
        }
        return null;
    }
}
