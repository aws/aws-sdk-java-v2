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

package software.amazon.awssdk.eventnotifications.s3.model;


import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;

/**
 * The LifecycleEventData is only visible for S3 Lifecycle transition events.
 */
@SdkPublicApi
public class LifecycleEventData {

    private final TransitionEventData transitionEventData;

    public LifecycleEventData(TransitionEventData transitionEventData) {
        this.transitionEventData = transitionEventData;
    }

    public TransitionEventData getTransitionEventData() {
        return transitionEventData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LifecycleEventData that = (LifecycleEventData) o;

        return Objects.equals(transitionEventData, that.transitionEventData);
    }

    @Override
    public int hashCode() {
        return transitionEventData != null ? transitionEventData.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("LifecycleEventDataEntity")
                       .add("transitionEventData", transitionEventData)
                       .build();
    }
}
