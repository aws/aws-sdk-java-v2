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

package software.amazon.awssdk.core.traits;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;

/**
 * Trait to include metadata about the marshalling/unmarshalling location (i.e. headers/payload/etc).
 */
@SdkProtectedApi
public final class LocationTrait implements Trait {

    private final MarshallLocation location;
    private final String locationName;
    private final String unmarshallLocationName;

    private LocationTrait(Builder builder) {
        this.location = builder.location;
        this.locationName = builder.locationName;
        this.unmarshallLocationName = builder.unmarshallLocationName == null ?
                                      builder.locationName : builder.unmarshallLocationName;
    }

    /**
     * @return Location of member (i.e. headers/query/path/payload).
     */
    public MarshallLocation location() {
        return location;
    }

    /**
     * @return Location name of member. I.E. the header or query param name, or the JSON field name, etc.
     */
    public String locationName() {
        return locationName;
    }

    /**
     * @return Location name for unmarshalling. This is only needed for the legacy EC2 protocol which has
     * different serialization/deserialization for the same fields.
     */
    public String unmarshallLocationName() {
        return unmarshallLocationName;
    }

    /**
     * @return Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LocationTrait}.
     */
    public static final class Builder {

        private MarshallLocation location;
        private String locationName;
        private String unmarshallLocationName;

        private Builder() {
        }

        public Builder location(MarshallLocation location) {
            this.location = location;
            return this;
        }

        public Builder locationName(String locationName) {
            this.locationName = locationName;
            return this;
        }

        public Builder unmarshallLocationName(String unmarshallLocationName) {
            this.unmarshallLocationName = unmarshallLocationName;
            return this;
        }

        public LocationTrait build() {
            return new LocationTrait(this);
        }

    }
}
