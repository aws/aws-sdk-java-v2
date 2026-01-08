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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/NestedContainers" target="_top">AWS API
 *      Documentation</a>
 */

public class NestedContainersRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private com.amazonaws.internal.SdkInternalList<java.util.List<String>> listOfListsOfStrings;

    private com.amazonaws.internal.SdkInternalList<java.util.List<SimpleStruct>> listOfListsOfStructs;

    private com.amazonaws.internal.SdkInternalList<java.util.List<AllTypesStructure>> listOfListsOfAllTypesStructs;

    private com.amazonaws.internal.SdkInternalList<java.util.List<java.util.List<String>>> listOfListOfListsOfStrings;

    private com.amazonaws.internal.SdkInternalMap<String, java.util.List<java.util.List<String>>> mapOfStringToListOfListsOfStrings;

    private String stringMember;

    /**
     * @return
     */

    public java.util.List<java.util.List<String>> getListOfListsOfStrings() {
        if (listOfListsOfStrings == null) {
            listOfListsOfStrings = new com.amazonaws.internal.SdkInternalList<java.util.List<String>>();
        }
        return listOfListsOfStrings;
    }

    /**
     * @param listOfListsOfStrings
     */

    public void setListOfListsOfStrings(java.util.Collection<java.util.List<String>> listOfListsOfStrings) {
        if (listOfListsOfStrings == null) {
            this.listOfListsOfStrings = null;
            return;
        }

        this.listOfListsOfStrings = new com.amazonaws.internal.SdkInternalList<java.util.List<String>>(listOfListsOfStrings);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfListsOfStrings(java.util.Collection)} or {@link #withListOfListsOfStrings(java.util.Collection)}
     * if you want to override the existing values.
     * </p>
     * 
     * @param listOfListsOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withListOfListsOfStrings(java.util.List<String>... listOfListsOfStrings) {
        if (this.listOfListsOfStrings == null) {
            setListOfListsOfStrings(new com.amazonaws.internal.SdkInternalList<java.util.List<String>>(listOfListsOfStrings.length));
        }
        for (java.util.List<String> ele : listOfListsOfStrings) {
            this.listOfListsOfStrings.add(ele);
        }
        return this;
    }

    /**
     * @param listOfListsOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withListOfListsOfStrings(java.util.Collection<java.util.List<String>> listOfListsOfStrings) {
        setListOfListsOfStrings(listOfListsOfStrings);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.util.List<SimpleStruct>> getListOfListsOfStructs() {
        if (listOfListsOfStructs == null) {
            listOfListsOfStructs = new com.amazonaws.internal.SdkInternalList<java.util.List<SimpleStruct>>();
        }
        return listOfListsOfStructs;
    }

    /**
     * @param listOfListsOfStructs
     */

    public void setListOfListsOfStructs(java.util.Collection<java.util.List<SimpleStruct>> listOfListsOfStructs) {
        if (listOfListsOfStructs == null) {
            this.listOfListsOfStructs = null;
            return;
        }

        this.listOfListsOfStructs = new com.amazonaws.internal.SdkInternalList<java.util.List<SimpleStruct>>(listOfListsOfStructs);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfListsOfStructs(java.util.Collection)} or {@link #withListOfListsOfStructs(java.util.Collection)}
     * if you want to override the existing values.
     * </p>
     * 
     * @param listOfListsOfStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withListOfListsOfStructs(java.util.List<SimpleStruct>... listOfListsOfStructs) {
        if (this.listOfListsOfStructs == null) {
            setListOfListsOfStructs(new com.amazonaws.internal.SdkInternalList<java.util.List<SimpleStruct>>(listOfListsOfStructs.length));
        }
        for (java.util.List<SimpleStruct> ele : listOfListsOfStructs) {
            this.listOfListsOfStructs.add(ele);
        }
        return this;
    }

    /**
     * @param listOfListsOfStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withListOfListsOfStructs(java.util.Collection<java.util.List<SimpleStruct>> listOfListsOfStructs) {
        setListOfListsOfStructs(listOfListsOfStructs);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.util.List<AllTypesStructure>> getListOfListsOfAllTypesStructs() {
        if (listOfListsOfAllTypesStructs == null) {
            listOfListsOfAllTypesStructs = new com.amazonaws.internal.SdkInternalList<java.util.List<AllTypesStructure>>();
        }
        return listOfListsOfAllTypesStructs;
    }

    /**
     * @param listOfListsOfAllTypesStructs
     */

    public void setListOfListsOfAllTypesStructs(java.util.Collection<java.util.List<AllTypesStructure>> listOfListsOfAllTypesStructs) {
        if (listOfListsOfAllTypesStructs == null) {
            this.listOfListsOfAllTypesStructs = null;
            return;
        }

        this.listOfListsOfAllTypesStructs = new com.amazonaws.internal.SdkInternalList<java.util.List<AllTypesStructure>>(listOfListsOfAllTypesStructs);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfListsOfAllTypesStructs(java.util.Collection)} or
     * {@link #withListOfListsOfAllTypesStructs(java.util.Collection)} if you want to override the existing values.
     * </p>
     * 
     * @param listOfListsOfAllTypesStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withListOfListsOfAllTypesStructs(java.util.List<AllTypesStructure>... listOfListsOfAllTypesStructs) {
        if (this.listOfListsOfAllTypesStructs == null) {
            setListOfListsOfAllTypesStructs(new com.amazonaws.internal.SdkInternalList<java.util.List<AllTypesStructure>>(listOfListsOfAllTypesStructs.length));
        }
        for (java.util.List<AllTypesStructure> ele : listOfListsOfAllTypesStructs) {
            this.listOfListsOfAllTypesStructs.add(ele);
        }
        return this;
    }

    /**
     * @param listOfListsOfAllTypesStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withListOfListsOfAllTypesStructs(java.util.Collection<java.util.List<AllTypesStructure>> listOfListsOfAllTypesStructs) {
        setListOfListsOfAllTypesStructs(listOfListsOfAllTypesStructs);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.util.List<java.util.List<String>>> getListOfListOfListsOfStrings() {
        if (listOfListOfListsOfStrings == null) {
            listOfListOfListsOfStrings = new com.amazonaws.internal.SdkInternalList<java.util.List<java.util.List<String>>>();
        }
        return listOfListOfListsOfStrings;
    }

    /**
     * @param listOfListOfListsOfStrings
     */

    public void setListOfListOfListsOfStrings(java.util.Collection<java.util.List<java.util.List<String>>> listOfListOfListsOfStrings) {
        if (listOfListOfListsOfStrings == null) {
            this.listOfListOfListsOfStrings = null;
            return;
        }

        this.listOfListOfListsOfStrings = new com.amazonaws.internal.SdkInternalList<java.util.List<java.util.List<String>>>(listOfListOfListsOfStrings);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfListOfListsOfStrings(java.util.Collection)} or
     * {@link #withListOfListOfListsOfStrings(java.util.Collection)} if you want to override the existing values.
     * </p>
     * 
     * @param listOfListOfListsOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withListOfListOfListsOfStrings(java.util.List<java.util.List<String>>... listOfListOfListsOfStrings) {
        if (this.listOfListOfListsOfStrings == null) {
            setListOfListOfListsOfStrings(new com.amazonaws.internal.SdkInternalList<java.util.List<java.util.List<String>>>(listOfListOfListsOfStrings.length));
        }
        for (java.util.List<java.util.List<String>> ele : listOfListOfListsOfStrings) {
            this.listOfListOfListsOfStrings.add(ele);
        }
        return this;
    }

    /**
     * @param listOfListOfListsOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withListOfListOfListsOfStrings(java.util.Collection<java.util.List<java.util.List<String>>> listOfListOfListsOfStrings) {
        setListOfListOfListsOfStrings(listOfListOfListsOfStrings);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, java.util.List<java.util.List<String>>> getMapOfStringToListOfListsOfStrings() {
        if (mapOfStringToListOfListsOfStrings == null) {
            mapOfStringToListOfListsOfStrings = new com.amazonaws.internal.SdkInternalMap<String, java.util.List<java.util.List<String>>>();
        }
        return mapOfStringToListOfListsOfStrings;
    }

    /**
     * @param mapOfStringToListOfListsOfStrings
     */

    public void setMapOfStringToListOfListsOfStrings(java.util.Map<String, java.util.List<java.util.List<String>>> mapOfStringToListOfListsOfStrings) {
        this.mapOfStringToListOfListsOfStrings = mapOfStringToListOfListsOfStrings == null ? null
                : new com.amazonaws.internal.SdkInternalMap<String, java.util.List<java.util.List<String>>>(mapOfStringToListOfListsOfStrings);
    }

    /**
     * @param mapOfStringToListOfListsOfStrings
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withMapOfStringToListOfListsOfStrings(
            java.util.Map<String, java.util.List<java.util.List<String>>> mapOfStringToListOfListsOfStrings) {
        setMapOfStringToListOfListsOfStrings(mapOfStringToListOfListsOfStrings);
        return this;
    }

    /**
     * Add a single MapOfStringToListOfListsOfStrings entry
     *
     * @see NestedContainersRequest#withMapOfStringToListOfListsOfStrings
     * @returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest addMapOfStringToListOfListsOfStringsEntry(String key, java.util.List<java.util.List<String>> value) {
        if (null == this.mapOfStringToListOfListsOfStrings) {
            this.mapOfStringToListOfListsOfStrings = new com.amazonaws.internal.SdkInternalMap<String, java.util.List<java.util.List<String>>>();
        }
        if (this.mapOfStringToListOfListsOfStrings.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.mapOfStringToListOfListsOfStrings.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into MapOfStringToListOfListsOfStrings.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest clearMapOfStringToListOfListsOfStringsEntries() {
        this.mapOfStringToListOfListsOfStrings = null;
        return this;
    }

    /**
     * @param stringMember
     */

    public void setStringMember(String stringMember) {
        this.stringMember = stringMember;
    }

    /**
     * @return
     */

    public String getStringMember() {
        return this.stringMember;
    }

    /**
     * @param stringMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public NestedContainersRequest withStringMember(String stringMember) {
        setStringMember(stringMember);
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
        if (getListOfListsOfStrings() != null)
            sb.append("ListOfListsOfStrings: ").append(getListOfListsOfStrings()).append(",");
        if (getListOfListsOfStructs() != null)
            sb.append("ListOfListsOfStructs: ").append(getListOfListsOfStructs()).append(",");
        if (getListOfListsOfAllTypesStructs() != null)
            sb.append("ListOfListsOfAllTypesStructs: ").append(getListOfListsOfAllTypesStructs()).append(",");
        if (getListOfListOfListsOfStrings() != null)
            sb.append("ListOfListOfListsOfStrings: ").append(getListOfListOfListsOfStrings()).append(",");
        if (getMapOfStringToListOfListsOfStrings() != null)
            sb.append("MapOfStringToListOfListsOfStrings: ").append(getMapOfStringToListOfListsOfStrings()).append(",");
        if (getStringMember() != null)
            sb.append("StringMember: ").append(getStringMember());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof NestedContainersRequest == false)
            return false;
        NestedContainersRequest other = (NestedContainersRequest) obj;
        if (other.getListOfListsOfStrings() == null ^ this.getListOfListsOfStrings() == null)
            return false;
        if (other.getListOfListsOfStrings() != null && other.getListOfListsOfStrings().equals(this.getListOfListsOfStrings()) == false)
            return false;
        if (other.getListOfListsOfStructs() == null ^ this.getListOfListsOfStructs() == null)
            return false;
        if (other.getListOfListsOfStructs() != null && other.getListOfListsOfStructs().equals(this.getListOfListsOfStructs()) == false)
            return false;
        if (other.getListOfListsOfAllTypesStructs() == null ^ this.getListOfListsOfAllTypesStructs() == null)
            return false;
        if (other.getListOfListsOfAllTypesStructs() != null && other.getListOfListsOfAllTypesStructs().equals(this.getListOfListsOfAllTypesStructs()) == false)
            return false;
        if (other.getListOfListOfListsOfStrings() == null ^ this.getListOfListOfListsOfStrings() == null)
            return false;
        if (other.getListOfListOfListsOfStrings() != null && other.getListOfListOfListsOfStrings().equals(this.getListOfListOfListsOfStrings()) == false)
            return false;
        if (other.getMapOfStringToListOfListsOfStrings() == null ^ this.getMapOfStringToListOfListsOfStrings() == null)
            return false;
        if (other.getMapOfStringToListOfListsOfStrings() != null
                && other.getMapOfStringToListOfListsOfStrings().equals(this.getMapOfStringToListOfListsOfStrings()) == false)
            return false;
        if (other.getStringMember() == null ^ this.getStringMember() == null)
            return false;
        if (other.getStringMember() != null && other.getStringMember().equals(this.getStringMember()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getListOfListsOfStrings() == null) ? 0 : getListOfListsOfStrings().hashCode());
        hashCode = prime * hashCode + ((getListOfListsOfStructs() == null) ? 0 : getListOfListsOfStructs().hashCode());
        hashCode = prime * hashCode + ((getListOfListsOfAllTypesStructs() == null) ? 0 : getListOfListsOfAllTypesStructs().hashCode());
        hashCode = prime * hashCode + ((getListOfListOfListsOfStrings() == null) ? 0 : getListOfListOfListsOfStrings().hashCode());
        hashCode = prime * hashCode + ((getMapOfStringToListOfListsOfStrings() == null) ? 0 : getMapOfStringToListOfListsOfStrings().hashCode());
        hashCode = prime * hashCode + ((getStringMember() == null) ? 0 : getStringMember().hashCode());
        return hashCode;
    }

    @Override
    public NestedContainersRequest clone() {
        return (NestedContainersRequest) super.clone();
    }

}
