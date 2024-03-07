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

package software.amazon.awssdk.eventnotifications.s3.internal;

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;

/**
 * Read Amazon S3 Event Notification in json format and marshal them into an instance of {@link S3EventNotification}.
 */
@SdkInternalApi
public interface S3EventNotificationReader {

    /**
     * Creates a S3EventNotificationReader
     * @return
     */
    static S3EventNotificationReader create() {
        return new DefaultS3EventNotificationReader();
    }

    /**
     * Read a json formatted Amazon S3 Event Notification from a UTF-8 string.
     * Will ignores all additional fields and missing fields will be set to null.
     *
     * @param event UTF-8 json of the notification.
     * @return S3EventNotification
     * @throws JsonParseException if json if malformed
     */
    S3EventNotification read(String event);

    /**
     * Read a json formatted Amazon S3 Event Notification from a UTF-8 InputStream.
     * Will ignores all additional fields and missing fields will be set to null.
     *
     * @param event UTF-8 json of the notification.
     * @return S3EventNotification
     * @throws JsonParseException if json if malformed
     */
    S3EventNotification read(InputStream event);

    /**
     * Read a json formatted Amazon S3 Event Notification from a UTF-8 encoded byte array.
     * Will ignores all additional fields and missing fields will be set to null.
     *
     * @param event UTF-8 json of the notification.
     * @return S3EventNotification
     * @throws JsonParseException if json if malformed
     */
    S3EventNotification read(byte[] event);
}
