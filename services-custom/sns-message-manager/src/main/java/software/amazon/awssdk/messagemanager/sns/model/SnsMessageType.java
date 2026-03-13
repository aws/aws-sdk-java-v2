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

package software.amazon.awssdk.messagemanager.sns.model;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The type of message sent by SNS. This corresponds to the value of the {@code Type} field in the SNS message. See the
 * <a href="https://docs.aws.amazon.com/sns/latest/dg/http-subscription-confirmation-json.html">SNS developer guide</a>
 * for more information.
 */
@SdkPublicApi
public enum SnsMessageType {
    SUBSCRIPTION_CONFIRMATION("SubscriptionConfirmation"),
    NOTIFICATION("Notification"),
    UNSUBSCRIBE_CONFIRMATION("UnsubscribeConfirmation"),

    /**
     * The type of the SNS message is unknown to this SDK version.
     */
    UNKNOWN("Unknown");

    private final String value;

    SnsMessageType(String value) {
        this.value = value;
    }

    public static SnsMessageType fromValue(String value) {
        for (SnsMessageType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }


    @Override
    public String toString() {
        return value;
    }
}
