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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

@SdkInternalApi
public interface S3EventNotificationReader {

    static S3EventNotificationReader create() {
        return new DefaultS3EventNotificationReader();
    }

    S3EventNotification read(String event);

    S3EventNotification read(InputStream event);

    S3EventNotification read(byte[] event);
}
