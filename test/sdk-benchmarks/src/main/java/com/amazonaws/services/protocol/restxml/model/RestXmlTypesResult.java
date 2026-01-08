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
package com.amazonaws.services.protocol.restxml.model;

import java.io.Serializable;


/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/RestXmlTypes" target="_top">AWS API
 *      Documentation</a>
 */

public class RestXmlTypesResult extends com.amazonaws.AmazonWebServiceResult<com.amazonaws.ResponseMetadata> implements Serializable, Cloneable {

    private java.util.List<String> flattenedListOfStrings;

    private java.util.List<String> nonFlattenedListWithLocation;

    private java.util.List<SimpleStruct> flattenedListOfStructs;

    private java.util.List<String> flattenedListWithLocation;

    private java.util.Map<String, String> flattenedMap;

    private java.util.Map<String, String> flattenedMapWithLocation;

    private java.util.Map<String, String> nonFlattenedMapWithLocation;

    private String stringMemberInQuery;

    private java.util.Date timestampMemberInHeader;

    private java.util.List<String> listOfStringsInQuery;

    private java.util.Map<String, String> mapOfStringToStringInQuery;

    /**
     * @return
     */

    public java.util.List<String> getFlattenedListOfStrings() {
        return flattenedListOfStrings;
    }

    /**
     * @param flattenedListOfStrings
     */

    public void setFlattenedListOfStrings(java.util.Collection<String> flattenedListOfStrings) {
        if (flattenedListOfStrings == null) {
            this.flattenedListOfStrings = null;
            return;
        }

        this.flattenedListOfStrings = new java.util.ArrayList<String>(flattenedListOfStrings);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setFlattenedListOfStrings(java.util.Collection)} or
     * {@link #withFlattenedListOfStrings(java.util.Collection)} if you want to override the existing values.
     * </p>
     * 
     * @param flattenedListOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withFlattenedListOfStrings(String... flattenedListOfStrings) {
        if (this.flattenedListOfStrings == null) {
            setFlattenedListOfStrings(new java.util.ArrayList<String>(flattenedListOfStrings.length));
        }
        for (String ele : flattenedListOfStrings) {
            this.flattenedListOfStrings.add(ele);
        }
        return this;
    }

    /**
     * @param flattenedListOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withFlattenedListOfStrings(java.util.Collection<String> flattenedListOfStrings) {
        setFlattenedListOfStrings(flattenedListOfStrings);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getNonFlattenedListWithLocation() {
        return nonFlattenedListWithLocation;
    }

    /**
     * @param nonFlattenedListWithLocation
     */

    public void setNonFlattenedListWithLocation(java.util.Collection<String> nonFlattenedListWithLocation) {
        if (nonFlattenedListWithLocation == null) {
            this.nonFlattenedListWithLocation = null;
            return;
        }

        this.nonFlattenedListWithLocation = new java.util.ArrayList<String>(nonFlattenedListWithLocation);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setNonFlattenedListWithLocation(java.util.Collection)} or
     * {@link #withNonFlattenedListWithLocation(java.util.Collection)} if you want to override the existing values.
     * </p>
     * 
     * @param nonFlattenedListWithLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withNonFlattenedListWithLocation(String... nonFlattenedListWithLocation) {
        if (this.nonFlattenedListWithLocation == null) {
            setNonFlattenedListWithLocation(new java.util.ArrayList<String>(nonFlattenedListWithLocation.length));
        }
        for (String ele : nonFlattenedListWithLocation) {
            this.nonFlattenedListWithLocation.add(ele);
        }
        return this;
    }

    /**
     * @param nonFlattenedListWithLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withNonFlattenedListWithLocation(java.util.Collection<String> nonFlattenedListWithLocation) {
        setNonFlattenedListWithLocation(nonFlattenedListWithLocation);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<SimpleStruct> getFlattenedListOfStructs() {
        return flattenedListOfStructs;
    }

    /**
     * @param flattenedListOfStructs
     */

    public void setFlattenedListOfStructs(java.util.Collection<SimpleStruct> flattenedListOfStructs) {
        if (flattenedListOfStructs == null) {
            this.flattenedListOfStructs = null;
            return;
        }

        this.flattenedListOfStructs = new java.util.ArrayList<SimpleStruct>(flattenedListOfStructs);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setFlattenedListOfStructs(java.util.Collection)} or
     * {@link #withFlattenedListOfStructs(java.util.Collection)} if you want to override the existing values.
     * </p>
     * 
     * @param flattenedListOfStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withFlattenedListOfStructs(SimpleStruct... flattenedListOfStructs) {
        if (this.flattenedListOfStructs == null) {
            setFlattenedListOfStructs(new java.util.ArrayList<SimpleStruct>(flattenedListOfStructs.length));
        }
        for (SimpleStruct ele : flattenedListOfStructs) {
            this.flattenedListOfStructs.add(ele);
        }
        return this;
    }

    /**
     * @param flattenedListOfStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withFlattenedListOfStructs(java.util.Collection<SimpleStruct> flattenedListOfStructs) {
        setFlattenedListOfStructs(flattenedListOfStructs);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getFlattenedListWithLocation() {
        return flattenedListWithLocation;
    }

    /**
     * @param flattenedListWithLocation
     */

    public void setFlattenedListWithLocation(java.util.Collection<String> flattenedListWithLocation) {
        if (flattenedListWithLocation == null) {
            this.flattenedListWithLocation = null;
            return;
        }

        this.flattenedListWithLocation = new java.util.ArrayList<String>(flattenedListWithLocation);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setFlattenedListWithLocation(java.util.Collection)} or
     * {@link #withFlattenedListWithLocation(java.util.Collection)} if you want to override the existing values.
     * </p>
     * 
     * @param flattenedListWithLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withFlattenedListWithLocation(String... flattenedListWithLocation) {
        if (this.flattenedListWithLocation == null) {
            setFlattenedListWithLocation(new java.util.ArrayList<String>(flattenedListWithLocation.length));
        }
        for (String ele : flattenedListWithLocation) {
            this.flattenedListWithLocation.add(ele);
        }
        return this;
    }

    /**
     * @param flattenedListWithLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withFlattenedListWithLocation(java.util.Collection<String> flattenedListWithLocation) {
        setFlattenedListWithLocation(flattenedListWithLocation);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, String> getFlattenedMap() {
        return flattenedMap;
    }

    /**
     * @param flattenedMap
     */

    public void setFlattenedMap(java.util.Map<String, String> flattenedMap) {
        this.flattenedMap = flattenedMap;
    }

    /**
     * @param flattenedMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withFlattenedMap(java.util.Map<String, String> flattenedMap) {
        setFlattenedMap(flattenedMap);
        return this;
    }

    /**
     * Add a single FlattenedMap entry
     *
     * @see RestXmlTypesResult#withFlattenedMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult addFlattenedMapEntry(String key, String value) {
        if (null == this.flattenedMap) {
            this.flattenedMap = new java.util.HashMap<String, String>();
        }
        if (this.flattenedMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.flattenedMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into FlattenedMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult clearFlattenedMapEntries() {
        this.flattenedMap = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, String> getFlattenedMapWithLocation() {
        return flattenedMapWithLocation;
    }

    /**
     * @param flattenedMapWithLocation
     */

    public void setFlattenedMapWithLocation(java.util.Map<String, String> flattenedMapWithLocation) {
        this.flattenedMapWithLocation = flattenedMapWithLocation;
    }

    /**
     * @param flattenedMapWithLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withFlattenedMapWithLocation(java.util.Map<String, String> flattenedMapWithLocation) {
        setFlattenedMapWithLocation(flattenedMapWithLocation);
        return this;
    }

    /**
     * Add a single FlattenedMapWithLocation entry
     *
     * @see RestXmlTypesResult#withFlattenedMapWithLocation
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult addFlattenedMapWithLocationEntry(String key, String value) {
        if (null == this.flattenedMapWithLocation) {
            this.flattenedMapWithLocation = new java.util.HashMap<String, String>();
        }
        if (this.flattenedMapWithLocation.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.flattenedMapWithLocation.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into FlattenedMapWithLocation.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult clearFlattenedMapWithLocationEntries() {
        this.flattenedMapWithLocation = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, String> getNonFlattenedMapWithLocation() {
        return nonFlattenedMapWithLocation;
    }

    /**
     * @param nonFlattenedMapWithLocation
     */

    public void setNonFlattenedMapWithLocation(java.util.Map<String, String> nonFlattenedMapWithLocation) {
        this.nonFlattenedMapWithLocation = nonFlattenedMapWithLocation;
    }

    /**
     * @param nonFlattenedMapWithLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withNonFlattenedMapWithLocation(java.util.Map<String, String> nonFlattenedMapWithLocation) {
        setNonFlattenedMapWithLocation(nonFlattenedMapWithLocation);
        return this;
    }

    /**
     * Add a single NonFlattenedMapWithLocation entry
     *
     * @see RestXmlTypesResult#withNonFlattenedMapWithLocation
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult addNonFlattenedMapWithLocationEntry(String key, String value) {
        if (null == this.nonFlattenedMapWithLocation) {
            this.nonFlattenedMapWithLocation = new java.util.HashMap<String, String>();
        }
        if (this.nonFlattenedMapWithLocation.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.nonFlattenedMapWithLocation.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into NonFlattenedMapWithLocation.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult clearNonFlattenedMapWithLocationEntries() {
        this.nonFlattenedMapWithLocation = null;
        return this;
    }

    /**
     * @param stringMemberInQuery
     */

    public void setStringMemberInQuery(String stringMemberInQuery) {
        this.stringMemberInQuery = stringMemberInQuery;
    }

    /**
     * @return
     */

    public String getStringMemberInQuery() {
        return this.stringMemberInQuery;
    }

    /**
     * @param stringMemberInQuery
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withStringMemberInQuery(String stringMemberInQuery) {
        setStringMemberInQuery(stringMemberInQuery);
        return this;
    }

    /**
     * @param timestampMemberInHeader
     */

    public void setTimestampMemberInHeader(java.util.Date timestampMemberInHeader) {
        this.timestampMemberInHeader = timestampMemberInHeader;
    }

    /**
     * @return
     */

    public java.util.Date getTimestampMemberInHeader() {
        return this.timestampMemberInHeader;
    }

    /**
     * @param timestampMemberInHeader
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withTimestampMemberInHeader(java.util.Date timestampMemberInHeader) {
        setTimestampMemberInHeader(timestampMemberInHeader);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getListOfStringsInQuery() {
        return listOfStringsInQuery;
    }

    /**
     * @param listOfStringsInQuery
     */

    public void setListOfStringsInQuery(java.util.Collection<String> listOfStringsInQuery) {
        if (listOfStringsInQuery == null) {
            this.listOfStringsInQuery = null;
            return;
        }

        this.listOfStringsInQuery = new java.util.ArrayList<String>(listOfStringsInQuery);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfStringsInQuery(java.util.Collection)} or {@link #withListOfStringsInQuery(java.util.Collection)}
     * if you want to override the existing values.
     * </p>
     * 
     * @param listOfStringsInQuery
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withListOfStringsInQuery(String... listOfStringsInQuery) {
        if (this.listOfStringsInQuery == null) {
            setListOfStringsInQuery(new java.util.ArrayList<String>(listOfStringsInQuery.length));
        }
        for (String ele : listOfStringsInQuery) {
            this.listOfStringsInQuery.add(ele);
        }
        return this;
    }

    /**
     * @param listOfStringsInQuery
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withListOfStringsInQuery(java.util.Collection<String> listOfStringsInQuery) {
        setListOfStringsInQuery(listOfStringsInQuery);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, String> getMapOfStringToStringInQuery() {
        return mapOfStringToStringInQuery;
    }

    /**
     * @param mapOfStringToStringInQuery
     */

    public void setMapOfStringToStringInQuery(java.util.Map<String, String> mapOfStringToStringInQuery) {
        this.mapOfStringToStringInQuery = mapOfStringToStringInQuery;
    }

    /**
     * @param mapOfStringToStringInQuery
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult withMapOfStringToStringInQuery(java.util.Map<String, String> mapOfStringToStringInQuery) {
        setMapOfStringToStringInQuery(mapOfStringToStringInQuery);
        return this;
    }

    /**
     * Add a single MapOfStringToStringInQuery entry
     *
     * @see RestXmlTypesResult#withMapOfStringToStringInQuery
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult addMapOfStringToStringInQueryEntry(String key, String value) {
        if (null == this.mapOfStringToStringInQuery) {
            this.mapOfStringToStringInQuery = new java.util.HashMap<String, String>();
        }
        if (this.mapOfStringToStringInQuery.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.mapOfStringToStringInQuery.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into MapOfStringToStringInQuery.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RestXmlTypesResult clearMapOfStringToStringInQueryEntries() {
        this.mapOfStringToStringInQuery = null;
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
        if (getFlattenedListOfStrings() != null)
            sb.append("FlattenedListOfStrings: ").append(getFlattenedListOfStrings()).append(",");
        if (getNonFlattenedListWithLocation() != null)
            sb.append("NonFlattenedListWithLocation: ").append(getNonFlattenedListWithLocation()).append(",");
        if (getFlattenedListOfStructs() != null)
            sb.append("FlattenedListOfStructs: ").append(getFlattenedListOfStructs()).append(",");
        if (getFlattenedListWithLocation() != null)
            sb.append("FlattenedListWithLocation: ").append(getFlattenedListWithLocation()).append(",");
        if (getFlattenedMap() != null)
            sb.append("FlattenedMap: ").append(getFlattenedMap()).append(",");
        if (getFlattenedMapWithLocation() != null)
            sb.append("FlattenedMapWithLocation: ").append(getFlattenedMapWithLocation()).append(",");
        if (getNonFlattenedMapWithLocation() != null)
            sb.append("NonFlattenedMapWithLocation: ").append(getNonFlattenedMapWithLocation()).append(",");
        if (getStringMemberInQuery() != null)
            sb.append("StringMemberInQuery: ").append(getStringMemberInQuery()).append(",");
        if (getTimestampMemberInHeader() != null)
            sb.append("TimestampMemberInHeader: ").append(getTimestampMemberInHeader()).append(",");
        if (getListOfStringsInQuery() != null)
            sb.append("ListOfStringsInQuery: ").append(getListOfStringsInQuery()).append(",");
        if (getMapOfStringToStringInQuery() != null)
            sb.append("MapOfStringToStringInQuery: ").append(getMapOfStringToStringInQuery());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof RestXmlTypesResult == false)
            return false;
        RestXmlTypesResult other = (RestXmlTypesResult) obj;
        if (other.getFlattenedListOfStrings() == null ^ this.getFlattenedListOfStrings() == null)
            return false;
        if (other.getFlattenedListOfStrings() != null && other.getFlattenedListOfStrings().equals(this.getFlattenedListOfStrings()) == false)
            return false;
        if (other.getNonFlattenedListWithLocation() == null ^ this.getNonFlattenedListWithLocation() == null)
            return false;
        if (other.getNonFlattenedListWithLocation() != null && other.getNonFlattenedListWithLocation().equals(this.getNonFlattenedListWithLocation()) == false)
            return false;
        if (other.getFlattenedListOfStructs() == null ^ this.getFlattenedListOfStructs() == null)
            return false;
        if (other.getFlattenedListOfStructs() != null && other.getFlattenedListOfStructs().equals(this.getFlattenedListOfStructs()) == false)
            return false;
        if (other.getFlattenedListWithLocation() == null ^ this.getFlattenedListWithLocation() == null)
            return false;
        if (other.getFlattenedListWithLocation() != null && other.getFlattenedListWithLocation().equals(this.getFlattenedListWithLocation()) == false)
            return false;
        if (other.getFlattenedMap() == null ^ this.getFlattenedMap() == null)
            return false;
        if (other.getFlattenedMap() != null && other.getFlattenedMap().equals(this.getFlattenedMap()) == false)
            return false;
        if (other.getFlattenedMapWithLocation() == null ^ this.getFlattenedMapWithLocation() == null)
            return false;
        if (other.getFlattenedMapWithLocation() != null && other.getFlattenedMapWithLocation().equals(this.getFlattenedMapWithLocation()) == false)
            return false;
        if (other.getNonFlattenedMapWithLocation() == null ^ this.getNonFlattenedMapWithLocation() == null)
            return false;
        if (other.getNonFlattenedMapWithLocation() != null && other.getNonFlattenedMapWithLocation().equals(this.getNonFlattenedMapWithLocation()) == false)
            return false;
        if (other.getStringMemberInQuery() == null ^ this.getStringMemberInQuery() == null)
            return false;
        if (other.getStringMemberInQuery() != null && other.getStringMemberInQuery().equals(this.getStringMemberInQuery()) == false)
            return false;
        if (other.getTimestampMemberInHeader() == null ^ this.getTimestampMemberInHeader() == null)
            return false;
        if (other.getTimestampMemberInHeader() != null && other.getTimestampMemberInHeader().equals(this.getTimestampMemberInHeader()) == false)
            return false;
        if (other.getListOfStringsInQuery() == null ^ this.getListOfStringsInQuery() == null)
            return false;
        if (other.getListOfStringsInQuery() != null && other.getListOfStringsInQuery().equals(this.getListOfStringsInQuery()) == false)
            return false;
        if (other.getMapOfStringToStringInQuery() == null ^ this.getMapOfStringToStringInQuery() == null)
            return false;
        if (other.getMapOfStringToStringInQuery() != null && other.getMapOfStringToStringInQuery().equals(this.getMapOfStringToStringInQuery()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getFlattenedListOfStrings() == null) ? 0 : getFlattenedListOfStrings().hashCode());
        hashCode = prime * hashCode + ((getNonFlattenedListWithLocation() == null) ? 0 : getNonFlattenedListWithLocation().hashCode());
        hashCode = prime * hashCode + ((getFlattenedListOfStructs() == null) ? 0 : getFlattenedListOfStructs().hashCode());
        hashCode = prime * hashCode + ((getFlattenedListWithLocation() == null) ? 0 : getFlattenedListWithLocation().hashCode());
        hashCode = prime * hashCode + ((getFlattenedMap() == null) ? 0 : getFlattenedMap().hashCode());
        hashCode = prime * hashCode + ((getFlattenedMapWithLocation() == null) ? 0 : getFlattenedMapWithLocation().hashCode());
        hashCode = prime * hashCode + ((getNonFlattenedMapWithLocation() == null) ? 0 : getNonFlattenedMapWithLocation().hashCode());
        hashCode = prime * hashCode + ((getStringMemberInQuery() == null) ? 0 : getStringMemberInQuery().hashCode());
        hashCode = prime * hashCode + ((getTimestampMemberInHeader() == null) ? 0 : getTimestampMemberInHeader().hashCode());
        hashCode = prime * hashCode + ((getListOfStringsInQuery() == null) ? 0 : getListOfStringsInQuery().hashCode());
        hashCode = prime * hashCode + ((getMapOfStringToStringInQuery() == null) ? 0 : getMapOfStringToStringInQuery().hashCode());
        return hashCode;
    }

    @Override
    public RestXmlTypesResult clone() {
        try {
            return (RestXmlTypesResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
