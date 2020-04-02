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

package software.amazon.awssdk.protocol.asserts.marshalling;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocol.model.SdkHttpMethodDeserializer;

/**
 * Main composite for marshalling assertions. Contains sub assertions for each component of an HTTP
 * request.
 */
public class SerializedAs extends CompositeMarshallingAssertion {

    public void setBody(RequestBodyAssertion body) {
        addAssertion(body);
    }

    public void setHeaders(HeadersAssertion headers) {
        addAssertion(headers);
    }

    public void setUri(String uri) {
        addAssertion(new UriAssertion(uri));
    }

    @JsonDeserialize(using = SdkHttpMethodDeserializer.class)
    public void setMethod(SdkHttpMethod method) {
        addAssertion(new HttpMethodAssertion(method));
    }

    public void setParams(QueryParamsAssertion params) {
        addAssertion(params);
    }

}
