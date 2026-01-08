/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.services.protocol.restjson.model;

import java.io.Serializable;


import com.amazonaws.AmazonWebServiceRequest;

/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MapOfStringToListOfStringInQueryParams"
 *      target="_top">AWS API Documentation</a>
 */

public class MapOfStringToListOfStringInQueryParamsRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private com.amazonaws.internal.SdkInternalMap<String, java.util.List<String>> mapOfStringToListOfStrings;

    /**
     * @return
     */

    public java.util.Map<String, java.util.List<String>> getMapOfStringToListOfStrings() {
        if (mapOfStringToListOfStrings == null) {
            mapOfStringToListOfStrings = new com.amazonaws.internal.SdkInternalMap<String, java.util.List<String>>();
        }
        return mapOfStringToListOfStrings;
    }

    /**
     * @param mapOfStringToListOfStrings
     */

    public void setMapOfStringToListOfStrings(java.util.Map<String, java.util.List<String>> mapOfStringToListOfStrings) {
        this.mapOfStringToListOfStrings = mapOfStringToListOfStrings == null ? null
                : new com.amazonaws.internal.SdkInternalMap<String, java.util.List<String>>(mapOfStringToListOfStrings);
    }

    /**
     * @param mapOfStringToListOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MapOfStringToListOfStringInQueryParamsRequest withMapOfStringToListOfStrings(java.util.Map<String, java.util.List<String>> mapOfStringToListOfStrings) {
        setMapOfStringToListOfStrings(mapOfStringToListOfStrings);
        return this;
    }

    /**
     * Add a single MapOfStringToListOfStrings entry
     *
     * @see MapOfStringToListOfStringInQueryParamsRequest#withMapOfStringToListOfStrings
     * @returns a reference to this object so that method calls can be chained together.
     */

    public MapOfStringToListOfStringInQueryParamsRequest addMapOfStringToListOfStringsEntry(String key, java.util.List<String> value) {
        if (null == this.mapOfStringToListOfStrings) {
            this.mapOfStringToListOfStrings = new com.amazonaws.internal.SdkInternalMap<String, java.util.List<String>>();
        }
        if (this.mapOfStringToListOfStrings.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.mapOfStringToListOfStrings.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into MapOfStringToListOfStrings.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MapOfStringToListOfStringInQueryParamsRequest clearMapOfStringToListOfStringsEntries() {
        this.mapOfStringToListOfStrings = null;
        return this;
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     *
     * @return A string representation of this object.
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getMapOfStringToListOfStrings() != null)
            sb.append("MapOfStringToListOfStrings: ").append(getMapOfStringToListOfStrings());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof MapOfStringToListOfStringInQueryParamsRequest == false)
            return false;
        MapOfStringToListOfStringInQueryParamsRequest other = (MapOfStringToListOfStringInQueryParamsRequest) obj;
        if (other.getMapOfStringToListOfStrings() == null ^ this.getMapOfStringToListOfStrings() == null)
            return false;
        if (other.getMapOfStringToListOfStrings() != null && other.getMapOfStringToListOfStrings().equals(this.getMapOfStringToListOfStrings()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getMapOfStringToListOfStrings() == null) ? 0 : getMapOfStringToListOfStrings().hashCode());
        return hashCode;
    }

    @Override
    public MapOfStringToListOfStringInQueryParamsRequest clone() {
        return (MapOfStringToListOfStringInQueryParamsRequest) super.clone();
    }

}
