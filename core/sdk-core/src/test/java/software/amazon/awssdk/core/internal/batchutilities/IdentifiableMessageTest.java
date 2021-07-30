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

package software.amazon.awssdk.core.internal.batchutilities;

import org.junit.Assert;
import org.junit.Test;

public class IdentifiableRequestAndResponseTest {

    @Test
    public void createIdentifiableRequest() {
        String id = "id";
        String request = "request";
        IdentifiableRequest<String> myRequest = new IdentifiableRequest<>(id, request);
        Assert.assertEquals(id, myRequest.id());
        Assert.assertEquals(request, myRequest.request());
    }

    @Test
    public void createIdentifiableResponse() {
        String id = "id";
        String response = "response";
        IdentifiableResponse<String> myResponse = new IdentifiableResponse<>(id, response);
        Assert.assertEquals(id, myResponse.id());
        Assert.assertEquals(response, myResponse.response());
    }

}
