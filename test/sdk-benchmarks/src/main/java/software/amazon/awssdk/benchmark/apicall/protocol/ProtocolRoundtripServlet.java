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
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Minimal servlet for protocol roundtrip benchmarks. Returns pre-loaded canned responses
 * with zero request inspection overhead. Routes are matched by URI prefix or X-Amz-Target header.
 */
class ProtocolRoundtripServlet extends HttpServlet {

    private final Map<String, CannedResponse> targetRoutes = new ConcurrentHashMap<>();
    private final Map<String, CannedResponse> uriRoutes = new ConcurrentHashMap<>();
    private CannedResponse defaultResponse;

    /**
     * Register a route matched by X-Amz-Target header value.
     */
    ProtocolRoundtripServlet routeByTarget(String target, String contentType, byte[] body) {
        targetRoutes.put(target, new CannedResponse(contentType, body));
        return this;
    }

    /**
     * Register a route matched by URI prefix.
     */
    ProtocolRoundtripServlet routeByUri(String uriPrefix, String contentType, byte[] body) {
        uriRoutes.put(uriPrefix, new CannedResponse(contentType, body));
        return this;
    }

    /**
     * Register a route matched by URI prefix with additional response headers.
     */
    ProtocolRoundtripServlet routeByUri(String uriPrefix, String contentType, byte[] body,
                                        Map<String, String> headers) {
        uriRoutes.put(uriPrefix, new CannedResponse(contentType, body, headers));
        return this;
    }

    /**
     * Default response when no route matches.
     */
    ProtocolRoundtripServlet defaultRoute(String contentType, byte[] body) {
        this.defaultResponse = new CannedResponse(contentType, body);
        return this;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Consume request body to simulate real HTTP exchange
        byte[] buf = new byte[8192];
        while (req.getInputStream().read(buf) != -1) {
            // drain
        }

        CannedResponse canned = resolve(req);
        if (canned == null) {
            resp.sendError(404);
            return;
        }

        resp.setStatus(200);
        resp.setContentType(canned.contentType);
        resp.setContentLength(canned.body.length);
        resp.setHeader("x-amzn-RequestId", "benchmark-request-id");
        if (canned.headers != null) {
            canned.headers.forEach(resp::setHeader);
        }
        try (OutputStream os = resp.getOutputStream()) {
            os.write(canned.body);
        }
    }

    private CannedResponse resolve(HttpServletRequest req) {
        // Try X-Amz-Target first (JSON/CBOR protocols)
        String target = req.getHeader("X-Amz-Target");
        if (target != null) {
            CannedResponse r = targetRoutes.get(target);
            if (r != null) {
                return r;
            }
        }

        // Try URI prefix match (REST protocols)
        String uri = req.getRequestURI();
        for (Map.Entry<String, CannedResponse> entry : uriRoutes.entrySet()) {
            if (uri.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return defaultResponse;
    }

    private static class CannedResponse {
        final String contentType;
        final byte[] body;
        final Map<String, String> headers;

        CannedResponse(String contentType, byte[] body) {
            this(contentType, body, null);
        }

        CannedResponse(String contentType, byte[] body, Map<String, String> headers) {
            this.contentType = contentType;
            this.body = body;
            this.headers = headers;
        }
    }
}
