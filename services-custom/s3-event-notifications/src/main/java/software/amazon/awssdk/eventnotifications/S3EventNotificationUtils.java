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

package software.amazon.awssdk.eventnotifications;

import java.util.Arrays;
import software.amazon.awssdk.eventnotifications.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.model.S3EventNotificationRecord;
import software.amazon.awssdk.services.s3.model.Event;

public class S3EventNotificationUtils {

    /**
     * <p>
     * Parse the JSON string into a S3EventNotification object.
     * </p>
     * <p>
     * The function will try its best to parse input JSON string as best as it can. It will not fail even if the JSON string
     * contains unknown properties. The function will throw SdkClientException if the input JSON string is not valid JSON.
     * </p>
     *
     * @param json JSON string to parse. Typically, this is the body of your SQS notification message body.
     * @return The resulting S3EventNotification object.
     */
    public S3EventNotification parseJson(String json) {
        // return Jackson.fromJsonString(json, S3EventNotification.class);
        // todo
        return null;
    }

    /**
     * @param
     * @return a JSON representation of the {@link S3EventNotification}
     */
    public String toJson(S3EventNotification s3EventNotification) {
        // return Jackson.toJsonString(this);
        // todo
        return null;
    }

    public Event getEventNameAsEnum(S3EventNotificationRecord eventNotificationRecord) {
        String value = eventNotificationRecord.getEventName();
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }

        String s3Prefix = "s3:";
        return Arrays.stream(Event.values())
                     .filter(entry -> entry.toString().equals(value) || entry.toString().equals(s3Prefix + value))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Cannot create enum from " + value + " value!"));
    }
}
