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

package software.amazon.awssdk.benchmark.apicall.protocol;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class ProtocolBenchmarkServlet extends HttpServlet {
    private final String putItemResponse;
    private final String queryResponse;
    private final String putObjectResponse;

    ProtocolBenchmarkServlet(String putItemResponse, String queryResponse, String putObjectResponse) {
        this.putItemResponse = putItemResponse;
        this.queryResponse = queryResponse;
        this.putObjectResponse = putObjectResponse;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String target = req.getHeader("X-Amz-Target");
        
        if ("DynamoDB_20120810.PutItem".equals(target)) {
            sendJsonResponse(resp, putItemResponse);
        } else if ("DynamoDB_20120810.Query".equals(target)) {
            sendJsonResponse(resp, queryResponse);
        } else {
            resp.sendError(404);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/test-bucket/test-key".equals(req.getRequestURI())) {
            // Consume the request body
            byte[] buffer = new byte[8192];
            while (req.getInputStream().read(buffer) != -1) {
                // Just consume
            }
            
            resp.setStatus(200);
            resp.setContentType("application/xml");
            resp.setHeader("ETag", "\"24346e1b50066607059af36e3b684b24\"");
            resp.setHeader("x-amz-version-id", "version123");
            resp.setHeader("x-amz-server-side-encryption", "AES256");
            
            try (PrintWriter writer = resp.getWriter()) {
                writer.write(putObjectResponse);
            }
        } else {
            resp.sendError(404);
        }
    }

    private void sendJsonResponse(HttpServletResponse resp, String body) throws IOException {
        resp.setStatus(200);
        resp.setContentType("application/x-amz-json-1.0");
        
        try (PrintWriter writer = resp.getWriter()) {
            writer.write(body);
        }
    }
}
