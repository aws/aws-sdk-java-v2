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

package software.amazon.awssdk.benchmark.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StreamingMockServlet extends HttpServlet {
    private static final byte[] STREAMING_RESPONSE_DATA = new byte[1024 * 1024]; // 1MB response

    static {
        // Initialize response data
        for (int i = 0; i < STREAMING_RESPONSE_DATA.length; i++) {
            STREAMING_RESPONSE_DATA[i] = (byte) (i % 256);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws  IOException {
        handleRequest(request, response);
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Check if this should be a streaming response
        if (isStreamingOperation(request)) {
            handleStreamingRequest(request, response);
        } else {
            handleJsonRequest(request, response);
        }
    }

    private boolean isStreamingOperation(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contentType = request.getContentType();

        return uri.contains("streaming") ||
               uri.contains("StreamingInput") ||
               uri.contains("StreamingOutput") ||
               "application/octet-stream".equals(contentType);
    }

    private void handleStreamingRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Consume input stream if present
        try (InputStream inputStream = request.getInputStream()) {
            byte[] buffer = new byte[8192];
            while (inputStream.read(buffer) != -1) {
                // Just consume the data
            }
        }

        // Send streaming response
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/octet-stream");
        response.setContentLength(STREAMING_RESPONSE_DATA.length);
        response.setHeader("x-amz-request-id", "streaming-" + System.currentTimeMillis());

        try (OutputStream outputStream = response.getOutputStream()) {
            outputStream.write(STREAMING_RESPONSE_DATA);
            outputStream.flush();
        }
    }

    private void handleJsonRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("x-amz-request-id", "json-" + System.currentTimeMillis());

        String jsonResponse = "{"
                              + "\"status\":\"success\","
                              + "\"message\":\"Mock operation completed\","
                              + "\"ResponseMetadata\":{"
                              + "\"RequestId\":\"mock-request-id\""
                              + "}"
                              + "}";

        try (PrintWriter writer = response.getWriter()) {
            writer.write(jsonResponse);
            writer.flush();
        }
    }
}