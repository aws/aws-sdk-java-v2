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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkInternalApi
public interface S3EventNotificationWriter
    extends ToCopyableBuilder<S3EventNotificationWriter.Builder, S3EventNotificationWriter> {

    String writeToString(S3EventNotification event);

    static S3EventNotificationWriter create() {
        return DefaultS3EventNotificationWriter.create();
    }

    static Builder builder() {
        return DefaultS3EventNotificationWriter.builder();
    }

    interface Builder extends CopyableBuilder<Builder, S3EventNotificationWriter> {
        /**
         * Configure whether the writer should "pretty-print" the output.
         * <p>
         * When set to true, this will add new lines and indentation to the output to make it easier for a human to read, at
         * the expense of extra data (white space) being output.
         * <p>
         * By default, this is {@code false}.
         */
        Builder prettyPrint(Boolean prettyPrint);
    }
}
