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

import com.amazonaws.protocol.StructuredPojo;
import com.amazonaws.protocol.ProtocolMarshaller;

/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/RecursiveStructType" target="_top">AWS API
 *      Documentation</a>
 */

public class RecursiveStructType implements Serializable, Cloneable, StructuredPojo {

    private String noRecurse;

    private RecursiveStructType recursiveStruct;

    private com.amazonaws.internal.SdkInternalList<RecursiveStructType> recursiveList;

    private com.amazonaws.internal.SdkInternalMap<String, RecursiveStructType> recursiveMap;

    /**
     * @param noRecurse
     */

    public void setNoRecurse(String noRecurse) {
        this.noRecurse = noRecurse;
    }

    /**
     * @return
     */

    public String getNoRecurse() {
        return this.noRecurse;
    }

    /**
     * @param noRecurse
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveStructType withNoRecurse(String noRecurse) {
        setNoRecurse(noRecurse);
        return this;
    }

    /**
     * @param recursiveStruct
     */

    public void setRecursiveStruct(RecursiveStructType recursiveStruct) {
        this.recursiveStruct = recursiveStruct;
    }

    /**
     * @return
     */

    public RecursiveStructType getRecursiveStruct() {
        return this.recursiveStruct;
    }

    /**
     * @param recursiveStruct
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveStructType withRecursiveStruct(RecursiveStructType recursiveStruct) {
        setRecursiveStruct(recursiveStruct);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<RecursiveStructType> getRecursiveList() {
        if (recursiveList == null) {
            recursiveList = new com.amazonaws.internal.SdkInternalList<RecursiveStructType>();
        }
        return recursiveList;
    }

    /**
     * @param recursiveList
     */

    public void setRecursiveList(java.util.Collection<RecursiveStructType> recursiveList) {
        if (recursiveList == null) {
            this.recursiveList = null;
            return;
        }

        this.recursiveList = new com.amazonaws.internal.SdkInternalList<RecursiveStructType>(recursiveList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setRecursiveList(java.util.Collection)} or {@link #withRecursiveList(java.util.Collection)} if you want
     * to override the existing values.
     * </p>
     * 
     * @param recursiveList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveStructType withRecursiveList(RecursiveStructType... recursiveList) {
        if (this.recursiveList == null) {
            setRecursiveList(new com.amazonaws.internal.SdkInternalList<RecursiveStructType>(recursiveList.length));
        }
        for (RecursiveStructType ele : recursiveList) {
            this.recursiveList.add(ele);
        }
        return this;
    }

    /**
     * @param recursiveList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveStructType withRecursiveList(java.util.Collection<RecursiveStructType> recursiveList) {
        setRecursiveList(recursiveList);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, RecursiveStructType> getRecursiveMap() {
        if (recursiveMap == null) {
            recursiveMap = new com.amazonaws.internal.SdkInternalMap<String, RecursiveStructType>();
        }
        return recursiveMap;
    }

    /**
     * @param recursiveMap
     */

    public void setRecursiveMap(java.util.Map<String, RecursiveStructType> recursiveMap) {
        this.recursiveMap = recursiveMap == null ? null : new com.amazonaws.internal.SdkInternalMap<String, RecursiveStructType>(recursiveMap);
    }

    /**
     * @param recursiveMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveStructType withRecursiveMap(java.util.Map<String, RecursiveStructType> recursiveMap) {
        setRecursiveMap(recursiveMap);
        return this;
    }

    /**
     * Add a single RecursiveMap entry
     *
     * @see RecursiveStructType#withRecursiveMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveStructType addRecursiveMapEntry(String key, RecursiveStructType value) {
        if (null == this.recursiveMap) {
            this.recursiveMap = new com.amazonaws.internal.SdkInternalMap<String, RecursiveStructType>();
        }
        if (this.recursiveMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.recursiveMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into RecursiveMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveStructType clearRecursiveMapEntries() {
        this.recursiveMap = null;
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
        if (getNoRecurse() != null)
            sb.append("NoRecurse: ").append(getNoRecurse()).append(",");
        if (getRecursiveStruct() != null)
            sb.append("RecursiveStruct: ").append(getRecursiveStruct()).append(",");
        if (getRecursiveList() != null)
            sb.append("RecursiveList: ").append(getRecursiveList()).append(",");
        if (getRecursiveMap() != null)
            sb.append("RecursiveMap: ").append(getRecursiveMap());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof RecursiveStructType == false)
            return false;
        RecursiveStructType other = (RecursiveStructType) obj;
        if (other.getNoRecurse() == null ^ this.getNoRecurse() == null)
            return false;
        if (other.getNoRecurse() != null && other.getNoRecurse().equals(this.getNoRecurse()) == false)
            return false;
        if (other.getRecursiveStruct() == null ^ this.getRecursiveStruct() == null)
            return false;
        if (other.getRecursiveStruct() != null && other.getRecursiveStruct().equals(this.getRecursiveStruct()) == false)
            return false;
        if (other.getRecursiveList() == null ^ this.getRecursiveList() == null)
            return false;
        if (other.getRecursiveList() != null && other.getRecursiveList().equals(this.getRecursiveList()) == false)
            return false;
        if (other.getRecursiveMap() == null ^ this.getRecursiveMap() == null)
            return false;
        if (other.getRecursiveMap() != null && other.getRecursiveMap().equals(this.getRecursiveMap()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getNoRecurse() == null) ? 0 : getNoRecurse().hashCode());
        hashCode = prime * hashCode + ((getRecursiveStruct() == null) ? 0 : getRecursiveStruct().hashCode());
        hashCode = prime * hashCode + ((getRecursiveList() == null) ? 0 : getRecursiveList().hashCode());
        hashCode = prime * hashCode + ((getRecursiveMap() == null) ? 0 : getRecursiveMap().hashCode());
        return hashCode;
    }

    @Override
    public RecursiveStructType clone() {
        try {
            return (RecursiveStructType) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.restjson.model.transform.RecursiveStructTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
