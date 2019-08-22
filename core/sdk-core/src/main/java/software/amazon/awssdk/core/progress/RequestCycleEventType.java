/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.progress;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Request life cycle events
 */
@SdkPublicApi
public enum RequestCycleEventType implements ProgressEventType {

    ////////////////////////////////////////
    // Generic Request/Response progress events
    ////////////////////////////////////////
    /**
     * Event indicating that the client has started sending the AWS API request.
     * This type of event is guaranteed to be only fired once during a
     * request-response cycle, even when the request is retried.
     */
    CLIENT_REQUEST_STARTED_EVENT,

    /**
     * Event indicating that the client has started sending the HTTP request.
     * The request progress listener will be notified of multiple instances of
     * this type of event if the request gets retried.
     */
    HTTP_REQUEST_STARTED_EVENT,

    /**
     * Event indicating that the client has finished sending the HTTP request.
     * The request progress listener will be notified of multiple instances of
     * this type of event if the request gets retried.
     */
    HTTP_REQUEST_COMPLETED_EVENT,

    /**
     * Event indicating that a failed request is detected as retryable and is
     * ready for the next retry.
     */
    CLIENT_REQUEST_RETRY_EVENT,

    /**
     * Event indicating that the client has started reading the HTTP response.
     * The request progress listener will be notified of this event only if the
     * client receives a successful service response (i.e. 2XX status code).
     */
    HTTP_RESPONSE_STARTED_EVENT,

    /**
     * Event indicating that the client has finished reading the HTTP response.
     * The request progress listener will be notified of this event only if the
     * client receives a successful service response (i.e. 2XX status code).
     */
    HTTP_RESPONSE_COMPLETED_EVENT,

    /**
     * Event indicating that the client has received a successful service
     * response and has finished parsing the response data.
     */
    CLIENT_REQUEST_SUCCESS_EVENT,

    /**
     * Event indicating that a client request has failed (after retries have
     * been conducted).
     */
    CLIENT_REQUEST_FAILED_EVENT,

    /**
     * Used to indicate the request body has been cancelled.
     */
    REQUEST_BODY_CANCEL_EVENT,

    /**
     * Used to indicate the request body is complete.
     */
    REQUEST_BODY_COMPLETE_EVENT,

    /**
     * Used to indicate the request has been reset.
     */
    REQUEST_BODY_RESET_EVENT,


    RESPONSE_BODY_CANCEL_EVENT,

    RESPONSE_BODY_COMPLETE_EVENT,

    RESPONSE_BODY_RESET_EVENT,
}
