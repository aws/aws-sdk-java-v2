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


import com.amazonaws.AmazonWebServiceRequest;

/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MembersInQueryParams" target="_top">AWS API
 *      Documentation</a>
 */

public class MembersInQueryParamsRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private String stringQueryParam;

    private Boolean booleanQueryParam;

    private Integer integerQueryParam;

    private Long longQueryParam;

    private Float floatQueryParam;

    private Double doubleQueryParam;

    private java.util.Date timestampQueryParam;

    private java.util.List<String> listOfStrings;

    private java.util.Map<String, String> mapOfStringToString;

    /**
     * @param stringQueryParam
     */

    public void setStringQueryParam(String stringQueryParam) {
        this.stringQueryParam = stringQueryParam;
    }

    /**
     * @return
     */

    public String getStringQueryParam() {
        return this.stringQueryParam;
    }

    /**
     * @param stringQueryParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withStringQueryParam(String stringQueryParam) {
        setStringQueryParam(stringQueryParam);
        return this;
    }

    /**
     * @param booleanQueryParam
     */

    public void setBooleanQueryParam(Boolean booleanQueryParam) {
        this.booleanQueryParam = booleanQueryParam;
    }

    /**
     * @return
     */

    public Boolean getBooleanQueryParam() {
        return this.booleanQueryParam;
    }

    /**
     * @param booleanQueryParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withBooleanQueryParam(Boolean booleanQueryParam) {
        setBooleanQueryParam(booleanQueryParam);
        return this;
    }

    /**
     * @return
     */

    public Boolean isBooleanQueryParam() {
        return this.booleanQueryParam;
    }

    /**
     * @param integerQueryParam
     */

    public void setIntegerQueryParam(Integer integerQueryParam) {
        this.integerQueryParam = integerQueryParam;
    }

    /**
     * @return
     */

    public Integer getIntegerQueryParam() {
        return this.integerQueryParam;
    }

    /**
     * @param integerQueryParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withIntegerQueryParam(Integer integerQueryParam) {
        setIntegerQueryParam(integerQueryParam);
        return this;
    }

    /**
     * @param longQueryParam
     */

    public void setLongQueryParam(Long longQueryParam) {
        this.longQueryParam = longQueryParam;
    }

    /**
     * @return
     */

    public Long getLongQueryParam() {
        return this.longQueryParam;
    }

    /**
     * @param longQueryParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withLongQueryParam(Long longQueryParam) {
        setLongQueryParam(longQueryParam);
        return this;
    }

    /**
     * @param floatQueryParam
     */

    public void setFloatQueryParam(Float floatQueryParam) {
        this.floatQueryParam = floatQueryParam;
    }

    /**
     * @return
     */

    public Float getFloatQueryParam() {
        return this.floatQueryParam;
    }

    /**
     * @param floatQueryParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withFloatQueryParam(Float floatQueryParam) {
        setFloatQueryParam(floatQueryParam);
        return this;
    }

    /**
     * @param doubleQueryParam
     */

    public void setDoubleQueryParam(Double doubleQueryParam) {
        this.doubleQueryParam = doubleQueryParam;
    }

    /**
     * @return
     */

    public Double getDoubleQueryParam() {
        return this.doubleQueryParam;
    }

    /**
     * @param doubleQueryParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withDoubleQueryParam(Double doubleQueryParam) {
        setDoubleQueryParam(doubleQueryParam);
        return this;
    }

    /**
     * @param timestampQueryParam
     */

    public void setTimestampQueryParam(java.util.Date timestampQueryParam) {
        this.timestampQueryParam = timestampQueryParam;
    }

    /**
     * @return
     */

    public java.util.Date getTimestampQueryParam() {
        return this.timestampQueryParam;
    }

    /**
     * @param timestampQueryParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withTimestampQueryParam(java.util.Date timestampQueryParam) {
        setTimestampQueryParam(timestampQueryParam);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getListOfStrings() {
        return listOfStrings;
    }

    /**
     * @param listOfStrings
     */

    public void setListOfStrings(java.util.Collection<String> listOfStrings) {
        if (listOfStrings == null) {
            this.listOfStrings = null;
            return;
        }

        this.listOfStrings = new java.util.ArrayList<String>(listOfStrings);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfStrings(java.util.Collection)} or {@link #withListOfStrings(java.util.Collection)} if you want
     * to override the existing values.
     * </p>
     * 
     * @param listOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withListOfStrings(String... listOfStrings) {
        if (this.listOfStrings == null) {
            setListOfStrings(new java.util.ArrayList<String>(listOfStrings.length));
        }
        for (String ele : listOfStrings) {
            this.listOfStrings.add(ele);
        }
        return this;
    }

    /**
     * @param listOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withListOfStrings(java.util.Collection<String> listOfStrings) {
        setListOfStrings(listOfStrings);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, String> getMapOfStringToString() {
        return mapOfStringToString;
    }

    /**
     * @param mapOfStringToString
     */

    public void setMapOfStringToString(java.util.Map<String, String> mapOfStringToString) {
        this.mapOfStringToString = mapOfStringToString;
    }

    /**
     * @param mapOfStringToString
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest withMapOfStringToString(java.util.Map<String, String> mapOfStringToString) {
        setMapOfStringToString(mapOfStringToString);
        return this;
    }

    /**
     * Add a single MapOfStringToString entry
     *
     * @see MembersInQueryParamsRequest#withMapOfStringToString
     * @returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest addMapOfStringToStringEntry(String key, String value) {
        if (null == this.mapOfStringToString) {
            this.mapOfStringToString = new java.util.HashMap<String, String>();
        }
        if (this.mapOfStringToString.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.mapOfStringToString.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into MapOfStringToString.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInQueryParamsRequest clearMapOfStringToStringEntries() {
        this.mapOfStringToString = null;
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
        if (getStringQueryParam() != null)
            sb.append("StringQueryParam: ").append(getStringQueryParam()).append(",");
        if (getBooleanQueryParam() != null)
            sb.append("BooleanQueryParam: ").append(getBooleanQueryParam()).append(",");
        if (getIntegerQueryParam() != null)
            sb.append("IntegerQueryParam: ").append(getIntegerQueryParam()).append(",");
        if (getLongQueryParam() != null)
            sb.append("LongQueryParam: ").append(getLongQueryParam()).append(",");
        if (getFloatQueryParam() != null)
            sb.append("FloatQueryParam: ").append(getFloatQueryParam()).append(",");
        if (getDoubleQueryParam() != null)
            sb.append("DoubleQueryParam: ").append(getDoubleQueryParam()).append(",");
        if (getTimestampQueryParam() != null)
            sb.append("TimestampQueryParam: ").append(getTimestampQueryParam()).append(",");
        if (getListOfStrings() != null)
            sb.append("ListOfStrings: ").append(getListOfStrings()).append(",");
        if (getMapOfStringToString() != null)
            sb.append("MapOfStringToString: ").append(getMapOfStringToString());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof MembersInQueryParamsRequest == false)
            return false;
        MembersInQueryParamsRequest other = (MembersInQueryParamsRequest) obj;
        if (other.getStringQueryParam() == null ^ this.getStringQueryParam() == null)
            return false;
        if (other.getStringQueryParam() != null && other.getStringQueryParam().equals(this.getStringQueryParam()) == false)
            return false;
        if (other.getBooleanQueryParam() == null ^ this.getBooleanQueryParam() == null)
            return false;
        if (other.getBooleanQueryParam() != null && other.getBooleanQueryParam().equals(this.getBooleanQueryParam()) == false)
            return false;
        if (other.getIntegerQueryParam() == null ^ this.getIntegerQueryParam() == null)
            return false;
        if (other.getIntegerQueryParam() != null && other.getIntegerQueryParam().equals(this.getIntegerQueryParam()) == false)
            return false;
        if (other.getLongQueryParam() == null ^ this.getLongQueryParam() == null)
            return false;
        if (other.getLongQueryParam() != null && other.getLongQueryParam().equals(this.getLongQueryParam()) == false)
            return false;
        if (other.getFloatQueryParam() == null ^ this.getFloatQueryParam() == null)
            return false;
        if (other.getFloatQueryParam() != null && other.getFloatQueryParam().equals(this.getFloatQueryParam()) == false)
            return false;
        if (other.getDoubleQueryParam() == null ^ this.getDoubleQueryParam() == null)
            return false;
        if (other.getDoubleQueryParam() != null && other.getDoubleQueryParam().equals(this.getDoubleQueryParam()) == false)
            return false;
        if (other.getTimestampQueryParam() == null ^ this.getTimestampQueryParam() == null)
            return false;
        if (other.getTimestampQueryParam() != null && other.getTimestampQueryParam().equals(this.getTimestampQueryParam()) == false)
            return false;
        if (other.getListOfStrings() == null ^ this.getListOfStrings() == null)
            return false;
        if (other.getListOfStrings() != null && other.getListOfStrings().equals(this.getListOfStrings()) == false)
            return false;
        if (other.getMapOfStringToString() == null ^ this.getMapOfStringToString() == null)
            return false;
        if (other.getMapOfStringToString() != null && other.getMapOfStringToString().equals(this.getMapOfStringToString()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getStringQueryParam() == null) ? 0 : getStringQueryParam().hashCode());
        hashCode = prime * hashCode + ((getBooleanQueryParam() == null) ? 0 : getBooleanQueryParam().hashCode());
        hashCode = prime * hashCode + ((getIntegerQueryParam() == null) ? 0 : getIntegerQueryParam().hashCode());
        hashCode = prime * hashCode + ((getLongQueryParam() == null) ? 0 : getLongQueryParam().hashCode());
        hashCode = prime * hashCode + ((getFloatQueryParam() == null) ? 0 : getFloatQueryParam().hashCode());
        hashCode = prime * hashCode + ((getDoubleQueryParam() == null) ? 0 : getDoubleQueryParam().hashCode());
        hashCode = prime * hashCode + ((getTimestampQueryParam() == null) ? 0 : getTimestampQueryParam().hashCode());
        hashCode = prime * hashCode + ((getListOfStrings() == null) ? 0 : getListOfStrings().hashCode());
        hashCode = prime * hashCode + ((getMapOfStringToString() == null) ? 0 : getMapOfStringToString().hashCode());
        return hashCode;
    }

    @Override
    public MembersInQueryParamsRequest clone() {
        return (MembersInQueryParamsRequest) super.clone();
    }

}
