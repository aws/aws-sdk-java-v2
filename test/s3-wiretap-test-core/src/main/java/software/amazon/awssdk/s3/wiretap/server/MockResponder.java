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

package software.amazon.awssdk.s3.wiretap.server;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface MockResponder {

    boolean matches(Request request);

    Response respond(Request request, byte[] body);

    default Response respond(Request request, CapturedBody body) {
        return respond(request, body.captured());
    }

    static MockResponder of(Predicate<Request> filter,
                            BiFunction<Request, byte[], Response> producer) {
        return new MockResponder() {
            @Override
            public boolean matches(Request request) {
                return filter.test(request);
            }

            @Override
            public Response respond(Request request, byte[] body) {
                return producer.apply(request, body);
            }
        };
    }
}
