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

package software.amazon.awssdk.core.exception;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.HttpStatusCode;

/**
 * Extension of SdkException that represents an error response returned by
 * the requested downstream service. Receiving an exception of this type indicates that
 * the caller's request was correctly transmitted to the service, but for some
 * reason, the service was not able to process it, and returned an error
 * response instead.
 * <p>
 * Exceptions that extend {@link SdkServiceException} are assumed to be able to be
 * successfully retried.
 * <p>
 * SdkServiceException provides callers several pieces of information that can
 * be used to obtain more information about the error and why it occurred.
 *
 * @see SdkClientException
 */
@SdkPublicApi
public class SdkServiceException extends SdkException {

    /**
     * The unique  identifier for the service request the caller made. The
     * request ID can uniquely identify the request and can be used by the
     * downstream service to identify specific caller requests to help
     * with debugging.
     */
    private String requestId;

    /**
     * The error message as returned by the service.
     */
    private String errorMessage;

    /**
     * The error code represented by this exception.
     */
    @ReviewBeforeRelease("Is this too specific to AWS?")
    private String errorCode;

    /**
     * {@link ErrorType} is a best effort determination of whether
     * or not a given {@link SdkServiceException} was caused by a
     * problem with the downstream service or a problem with the clients
     * request.
     * <p>
     * Requests marked as {@link ErrorType#CLIENT} indicate a problem
     * with the request that the caller made. These requests are not expected
     * to be able to succeed on a retry.
     * <p>
     * Requests marked with {@link ErrorType#SERVICE} indicate a problem
     * on the service side. These requests may be able to succeed on a retry.
     * <p>
     * Requests that can't be determined to be either service or client errors
     * are marked as {@link ErrorType#UNKNOWN}. These requests may be able to be
     * safely retried.
     */
    private ErrorType errorType = ErrorType.UNKNOWN;

    /**
     * The HTTP status code that was returned with this error.
     */
    private int statusCode;

    /**
     * The name of the service that sent this error response.
     */
    private String serviceName;

    /**
     * All of HTTP headers that were returned in the response.
     */
    private Map<String, String> headers;

    /**
     * The raw payload of the HTTP response.
     */
    private byte[] rawResponse;

    /**
     * Constructs a new SdkServiceException with the specified message.
     *
     * @param errorMessage
     *            An error message describing what went wrong.
     */
    public SdkServiceException(String errorMessage) {
        super(errorMessage);
        this.errorMessage = errorMessage;
    }

    /**
     * Constructs a new SdkServiceException with the specified message and
     * exception indicating the root cause.
     *
     * @param errorMessage
     *            An error message describing what went wrong.
     * @param cause
     *            The root exception that caused this exception to be thrown.
     */
    public SdkServiceException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the request ID that uniquely identifies the service request
     * the caller made.
     *
     * @return The request ID that uniquely identifies the service request
     *         the caller made.
     */
    public String requestId() {
        return requestId;
    }

    public void requestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Returns the name of the service that sent this error response.
     *
     * @return The name of the service that sent this error response.
     */
    public String serviceName() {
        return serviceName;
    }

    public void serviceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * @return the human-readable error message provided by the service
     */
    public String errorMessage() {
        return errorMessage;
    }

    public void errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the error code associated with the response.
     */
    public String errorCode() {
        return errorCode;
    }

    public void errorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Returns the {@link ErrorType} associated with the request.
     */
    public ErrorType errorType() {
        return errorType;
    }

    public void errorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    /**
     * Returns the HTTP status code that was returned with this service
     * exception.
     */
    public int statusCode() {
        return statusCode;
    }

    public void statusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Returns the response payload as bytes.
     */
    public byte[] rawResponse() {
        return rawResponse == null ? null : rawResponse.clone();
    }

    public void rawResponse(byte[] rawResponse) {
        if (rawResponse != null) {
            this.rawResponse = rawResponse.clone();
        }
    }

    /**
     * Returns a map of HTTP headers associated with the error response.
     */
    public Map<String, String> headers() {
        return headers;
    }

    public void headers(Map<String, String> headers) {
        this.headers = Collections.unmodifiableMap(headers);
    }

    @Override
    public String getMessage() {
        return errorMessage()
               + " (Service: " + serviceName()
               + "; Status Code: " + statusCode()
               + "; Request ID: " + requestId() + ")";
    }

    /**
     * Specifies whether or not an exception is caused by clock skew.
     */
    public boolean isClockSkewException() {
        return false;
    }

    /**
     * Specifies whether or not an exception is caused by throttling.
     *
     * @return true if the status code is 429, otherwise false.
     */
    public boolean isThrottlingException() {
        return statusCode() == HttpStatusCode.THROTTLING;
    }

}
