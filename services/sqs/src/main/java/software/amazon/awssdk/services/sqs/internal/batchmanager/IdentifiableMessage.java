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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Wrapper class for a message (either request/response) and its associated batch id.
 *
 * @param <MessageT> The message
 */
@SdkInternalApi
public final class IdentifiableMessage<MessageT> {

    private final String id;
    private final MessageT message;

    public IdentifiableMessage(String id, MessageT message) {
        this.id = Validate.notNull(id, "ID cannot be null");
        this.message = Validate.notNull(message, "Message cannot be null");
    }

    public String id() {
        return id;
    }

    public MessageT message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdentifiableMessage<?> that = (IdentifiableMessage<?>) o;

        if (!id.equals(that.id)) {
            return false;
        }
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }
}
