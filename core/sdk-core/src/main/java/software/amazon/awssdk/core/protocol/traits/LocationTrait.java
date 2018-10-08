/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.protocol.traits;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;

/**
 * Trait to include metadata about the marshalling/unmarshalling location (i.e. headers/payload/etc).
 */
@SdkProtectedApi
public final class LocationTrait implements Trait {

    private final MarshallLocation location;
    private final String locationName;

    private LocationTrait(MarshallLocation location, String locationName) {
        this.location = location;
        this.locationName = locationName;
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

    public static LocationTrait create(MarshallLocation location, String locationName) {
        return new LocationTrait(location, locationName);
    }
}
