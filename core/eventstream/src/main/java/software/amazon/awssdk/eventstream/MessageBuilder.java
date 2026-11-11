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

package software.amazon.awssdk.eventstream;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * An abstraction for message construction. This interface can be used to
 * customize message creation in different ways, such as implementing
 * decorators that add headers or manipulate the message payload.
 */
@SdkProtectedApi
@FunctionalInterface
public interface MessageBuilder {
    Message build(Map<String, HeaderValue> headers, byte[] payload);

    static MessageBuilder defaultBuilder() {
        return Message::new;
    }
}
