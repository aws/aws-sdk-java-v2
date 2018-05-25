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

package software.amazon.awssdk.http;

/**
 * Constants for common HTTP status codes.
 */
public final class HttpStatusCodes {

    // --- 1xx Informational ---

    public static final int CONTINUE = 100;

    // --- 2xx Success ---

    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int ACCEPTED = 202;
    public static final int NON_AUTHORITATIVE_INFORMATION = 203;
    public static final int NO_CONTENT = 204;
    public static final int RESET_CONTENT = 205;
    public static final int PARTIAL_CONTENT = 206;

    // --- 3xx Redirection ---

    public static final int MOVED_PERMANENTLY = 301;
    public static final int MOVED_TEMPORARILY = 302;
    public static final int TEMPORARY_REDIRECT = 307;

    // --- 4xx Client Error ---

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int NOT_ACCEPTABLE = 406;
    public static final int REQUEST_TIMEOUT = 408;
    public static final int REQUEST_TOO_LONG = 413;
    public static final int THROTTLING = 429;

    // --- 5xx Server Error ---

    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int BAD_GATEWAY = 502;
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final int GATEWAY_TIMEOUT = 504;

    private HttpStatusCodes() {}
}
