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

package software.amazon.awssdk.services.dynamodb.document.spec;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;

/**
 * Abstract implementation base class for parameter specification.
 *
 * @param <T> request type
 */
class AbstractSpec<T extends AmazonWebServiceRequest> {
    private T req;

    AbstractSpec(T req) {
        setRequest(req);
    }

    public void setRequest(T req) {
        InternalUtils.applyUserAgent(req);
        this.req = req;
    }

    /**
     * Internal method.  Not meant to be called directly.  May change without notice.
     */
    public T getRequest() {
        return req;
    }

    public ProgressListener getProgressListener() {
        return getRequest().getGeneralProgressListener();
    }

    public void setProgressListener(ProgressListener progressListener) {
        getRequest().setGeneralProgressListener(progressListener);
    }

    public RequestMetricCollector getRequestMetricCollector() {
        return getRequest().getRequestMetricCollector();
    }

    public void setRequestMetricCollector(
            RequestMetricCollector requestMetricCollector) {
        getRequest().setRequestMetricCollector(requestMetricCollector);
    }

    public AbstractSpec<T> withProgressListener(
            ProgressListener progressListener) {
        getRequest().setGeneralProgressListener(progressListener);
        return this;
    }

    public AbstractSpec<T> withRequestMetricCollector(
            RequestMetricCollector requestMetricCollector) {
        getRequest().setRequestMetricCollector(requestMetricCollector);
        return this;
    }
}
