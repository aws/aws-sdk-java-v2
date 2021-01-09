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

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.JSON_BODY;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Always succeeds with with a 200 response.
 */
public class AlwaysSuccessServlet extends HttpServlet {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("application/json");
        response.setContentLength(JSON_BODY.getBytes(StandardCharsets.UTF_8).length);
        response.getOutputStream().print(JSON_BODY);
    }
}
