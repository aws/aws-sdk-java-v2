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

@SdkPublicApi
public class TransitionEventData {

    private final String destinationStorageClass;

    public TransitionEventData(String destinationStorageClass) {
        this.destinationStorageClass = destinationStorageClass;
    }

    public String getDestinationStorageClass() {
        return destinationStorageClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransitionEventData that = (TransitionEventData) o;

        return Objects.equals(destinationStorageClass, that.destinationStorageClass);
    }

    @Override
    public int hashCode() {
        return destinationStorageClass != null ? destinationStorageClass.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("TransitionEventData")
                       .add("destinationStorageClass", destinationStorageClass)
                       .build();
    }
}
