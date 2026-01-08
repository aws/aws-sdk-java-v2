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
package com.amazonaws.services.protocol.rpcv2protocol.model;

import java.io.Serializable;


/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborLists" target="_top">AWS API
 *      Documentation</a>
 */

public class RpcV2CborListsResult extends com.amazonaws.AmazonWebServiceResult<com.amazonaws.ResponseMetadata> implements Serializable, Cloneable {

    private java.util.List<String> stringList;

    private java.util.List<String> stringSet;

    private java.util.List<Integer> integerList;

    private java.util.List<Boolean> booleanList;

    private java.util.List<java.util.Date> timestampList;

    private java.util.List<String> enumList;

    private java.util.List<Integer> intEnumList;

    private java.util.List<java.util.List<String>> nestedStringList;

    private java.util.List<StructureListMember> structureList;

    private java.util.List<java.nio.ByteBuffer> blobList;

    /**
     * @return
     */

    public java.util.List<String> getStringList() {
        return stringList;
    }

    /**
     * @param stringList
     */

    public void setStringList(java.util.Collection<String> stringList) {
        if (stringList == null) {
            this.stringList = null;
            return;
        }

        this.stringList = new java.util.ArrayList<String>(stringList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setStringList(java.util.Collection)} or {@link #withStringList(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param stringList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withStringList(String... stringList) {
        if (this.stringList == null) {
            setStringList(new java.util.ArrayList<String>(stringList.length));
        }
        for (String ele : stringList) {
            this.stringList.add(ele);
        }
        return this;
    }

    /**
     * @param stringList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withStringList(java.util.Collection<String> stringList) {
        setStringList(stringList);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getStringSet() {
        return stringSet;
    }

    /**
     * @param stringSet
     */

    public void setStringSet(java.util.Collection<String> stringSet) {
        if (stringSet == null) {
            this.stringSet = null;
            return;
        }

        this.stringSet = new java.util.ArrayList<String>(stringSet);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setStringSet(java.util.Collection)} or {@link #withStringSet(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param stringSet
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withStringSet(String... stringSet) {
        if (this.stringSet == null) {
            setStringSet(new java.util.ArrayList<String>(stringSet.length));
        }
        for (String ele : stringSet) {
            this.stringSet.add(ele);
        }
        return this;
    }

    /**
     * @param stringSet
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withStringSet(java.util.Collection<String> stringSet) {
        setStringSet(stringSet);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<Integer> getIntegerList() {
        return integerList;
    }

    /**
     * @param integerList
     */

    public void setIntegerList(java.util.Collection<Integer> integerList) {
        if (integerList == null) {
            this.integerList = null;
            return;
        }

        this.integerList = new java.util.ArrayList<Integer>(integerList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setIntegerList(java.util.Collection)} or {@link #withIntegerList(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param integerList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withIntegerList(Integer... integerList) {
        if (this.integerList == null) {
            setIntegerList(new java.util.ArrayList<Integer>(integerList.length));
        }
        for (Integer ele : integerList) {
            this.integerList.add(ele);
        }
        return this;
    }

    /**
     * @param integerList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withIntegerList(java.util.Collection<Integer> integerList) {
        setIntegerList(integerList);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<Boolean> getBooleanList() {
        return booleanList;
    }

    /**
     * @param booleanList
     */

    public void setBooleanList(java.util.Collection<Boolean> booleanList) {
        if (booleanList == null) {
            this.booleanList = null;
            return;
        }

        this.booleanList = new java.util.ArrayList<Boolean>(booleanList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setBooleanList(java.util.Collection)} or {@link #withBooleanList(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param booleanList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withBooleanList(Boolean... booleanList) {
        if (this.booleanList == null) {
            setBooleanList(new java.util.ArrayList<Boolean>(booleanList.length));
        }
        for (Boolean ele : booleanList) {
            this.booleanList.add(ele);
        }
        return this;
    }

    /**
     * @param booleanList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withBooleanList(java.util.Collection<Boolean> booleanList) {
        setBooleanList(booleanList);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.util.Date> getTimestampList() {
        return timestampList;
    }

    /**
     * @param timestampList
     */

    public void setTimestampList(java.util.Collection<java.util.Date> timestampList) {
        if (timestampList == null) {
            this.timestampList = null;
            return;
        }

        this.timestampList = new java.util.ArrayList<java.util.Date>(timestampList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setTimestampList(java.util.Collection)} or {@link #withTimestampList(java.util.Collection)} if you want
     * to override the existing values.
     * </p>
     * 
     * @param timestampList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withTimestampList(java.util.Date... timestampList) {
        if (this.timestampList == null) {
            setTimestampList(new java.util.ArrayList<java.util.Date>(timestampList.length));
        }
        for (java.util.Date ele : timestampList) {
            this.timestampList.add(ele);
        }
        return this;
    }

    /**
     * @param timestampList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withTimestampList(java.util.Collection<java.util.Date> timestampList) {
        setTimestampList(timestampList);
        return this;
    }

    /**
     * @return
     * @see FooEnum
     */

    public java.util.List<String> getEnumList() {
        return enumList;
    }

    /**
     * @param enumList
     * @see FooEnum
     */

    public void setEnumList(java.util.Collection<String> enumList) {
        if (enumList == null) {
            this.enumList = null;
            return;
        }

        this.enumList = new java.util.ArrayList<String>(enumList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setEnumList(java.util.Collection)} or {@link #withEnumList(java.util.Collection)} if you want to override
     * the existing values.
     * </p>
     * 
     * @param enumList
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see FooEnum
     */

    public RpcV2CborListsResult withEnumList(String... enumList) {
        if (this.enumList == null) {
            setEnumList(new java.util.ArrayList<String>(enumList.length));
        }
        for (String ele : enumList) {
            this.enumList.add(ele);
        }
        return this;
    }

    /**
     * @param enumList
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see FooEnum
     */

    public RpcV2CborListsResult withEnumList(java.util.Collection<String> enumList) {
        setEnumList(enumList);
        return this;
    }

    /**
     * @param enumList
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see FooEnum
     */

    public RpcV2CborListsResult withEnumList(FooEnum... enumList) {
        java.util.ArrayList<String> enumListCopy = new java.util.ArrayList<String>(enumList.length);
        for (FooEnum value : enumList) {
            enumListCopy.add(value.toString());
        }
        if (getEnumList() == null) {
            setEnumList(enumListCopy);
        } else {
            getEnumList().addAll(enumListCopy);
        }
        return this;
    }

    /**
     * @return
     */

    public java.util.List<Integer> getIntEnumList() {
        return intEnumList;
    }

    /**
     * @param intEnumList
     */

    public void setIntEnumList(java.util.Collection<Integer> intEnumList) {
        if (intEnumList == null) {
            this.intEnumList = null;
            return;
        }

        this.intEnumList = new java.util.ArrayList<Integer>(intEnumList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setIntEnumList(java.util.Collection)} or {@link #withIntEnumList(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param intEnumList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withIntEnumList(Integer... intEnumList) {
        if (this.intEnumList == null) {
            setIntEnumList(new java.util.ArrayList<Integer>(intEnumList.length));
        }
        for (Integer ele : intEnumList) {
            this.intEnumList.add(ele);
        }
        return this;
    }

    /**
     * @param intEnumList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withIntEnumList(java.util.Collection<Integer> intEnumList) {
        setIntEnumList(intEnumList);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.util.List<String>> getNestedStringList() {
        return nestedStringList;
    }

    /**
     * @param nestedStringList
     */

    public void setNestedStringList(java.util.Collection<java.util.List<String>> nestedStringList) {
        if (nestedStringList == null) {
            this.nestedStringList = null;
            return;
        }

        this.nestedStringList = new java.util.ArrayList<java.util.List<String>>(nestedStringList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setNestedStringList(java.util.Collection)} or {@link #withNestedStringList(java.util.Collection)} if you
     * want to override the existing values.
     * </p>
     * 
     * @param nestedStringList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withNestedStringList(java.util.List<String>... nestedStringList) {
        if (this.nestedStringList == null) {
            setNestedStringList(new java.util.ArrayList<java.util.List<String>>(nestedStringList.length));
        }
        for (java.util.List<String> ele : nestedStringList) {
            this.nestedStringList.add(ele);
        }
        return this;
    }

    /**
     * @param nestedStringList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withNestedStringList(java.util.Collection<java.util.List<String>> nestedStringList) {
        setNestedStringList(nestedStringList);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<StructureListMember> getStructureList() {
        return structureList;
    }

    /**
     * @param structureList
     */

    public void setStructureList(java.util.Collection<StructureListMember> structureList) {
        if (structureList == null) {
            this.structureList = null;
            return;
        }

        this.structureList = new java.util.ArrayList<StructureListMember>(structureList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setStructureList(java.util.Collection)} or {@link #withStructureList(java.util.Collection)} if you want
     * to override the existing values.
     * </p>
     * 
     * @param structureList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withStructureList(StructureListMember... structureList) {
        if (this.structureList == null) {
            setStructureList(new java.util.ArrayList<StructureListMember>(structureList.length));
        }
        for (StructureListMember ele : structureList) {
            this.structureList.add(ele);
        }
        return this;
    }

    /**
     * @param structureList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withStructureList(java.util.Collection<StructureListMember> structureList) {
        setStructureList(structureList);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.nio.ByteBuffer> getBlobList() {
        return blobList;
    }

    /**
     * @param blobList
     */

    public void setBlobList(java.util.Collection<java.nio.ByteBuffer> blobList) {
        if (blobList == null) {
            this.blobList = null;
            return;
        }

        this.blobList = new java.util.ArrayList<java.nio.ByteBuffer>(blobList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setBlobList(java.util.Collection)} or {@link #withBlobList(java.util.Collection)} if you want to override
     * the existing values.
     * </p>
     * 
     * @param blobList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withBlobList(java.nio.ByteBuffer... blobList) {
        if (this.blobList == null) {
            setBlobList(new java.util.ArrayList<java.nio.ByteBuffer>(blobList.length));
        }
        for (java.nio.ByteBuffer ele : blobList) {
            this.blobList.add(ele);
        }
        return this;
    }

    /**
     * @param blobList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborListsResult withBlobList(java.util.Collection<java.nio.ByteBuffer> blobList) {
        setBlobList(blobList);
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
        if (getStringList() != null)
            sb.append("StringList: ").append(getStringList()).append(",");
        if (getStringSet() != null)
            sb.append("StringSet: ").append(getStringSet()).append(",");
        if (getIntegerList() != null)
            sb.append("IntegerList: ").append(getIntegerList()).append(",");
        if (getBooleanList() != null)
            sb.append("BooleanList: ").append(getBooleanList()).append(",");
        if (getTimestampList() != null)
            sb.append("TimestampList: ").append(getTimestampList()).append(",");
        if (getEnumList() != null)
            sb.append("EnumList: ").append(getEnumList()).append(",");
        if (getIntEnumList() != null)
            sb.append("IntEnumList: ").append(getIntEnumList()).append(",");
        if (getNestedStringList() != null)
            sb.append("NestedStringList: ").append(getNestedStringList()).append(",");
        if (getStructureList() != null)
            sb.append("StructureList: ").append(getStructureList()).append(",");
        if (getBlobList() != null)
            sb.append("BlobList: ").append(getBlobList());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof RpcV2CborListsResult == false)
            return false;
        RpcV2CborListsResult other = (RpcV2CborListsResult) obj;
        if (other.getStringList() == null ^ this.getStringList() == null)
            return false;
        if (other.getStringList() != null && other.getStringList().equals(this.getStringList()) == false)
            return false;
        if (other.getStringSet() == null ^ this.getStringSet() == null)
            return false;
        if (other.getStringSet() != null && other.getStringSet().equals(this.getStringSet()) == false)
            return false;
        if (other.getIntegerList() == null ^ this.getIntegerList() == null)
            return false;
        if (other.getIntegerList() != null && other.getIntegerList().equals(this.getIntegerList()) == false)
            return false;
        if (other.getBooleanList() == null ^ this.getBooleanList() == null)
            return false;
        if (other.getBooleanList() != null && other.getBooleanList().equals(this.getBooleanList()) == false)
            return false;
        if (other.getTimestampList() == null ^ this.getTimestampList() == null)
            return false;
        if (other.getTimestampList() != null && other.getTimestampList().equals(this.getTimestampList()) == false)
            return false;
        if (other.getEnumList() == null ^ this.getEnumList() == null)
            return false;
        if (other.getEnumList() != null && other.getEnumList().equals(this.getEnumList()) == false)
            return false;
        if (other.getIntEnumList() == null ^ this.getIntEnumList() == null)
            return false;
        if (other.getIntEnumList() != null && other.getIntEnumList().equals(this.getIntEnumList()) == false)
            return false;
        if (other.getNestedStringList() == null ^ this.getNestedStringList() == null)
            return false;
        if (other.getNestedStringList() != null && other.getNestedStringList().equals(this.getNestedStringList()) == false)
            return false;
        if (other.getStructureList() == null ^ this.getStructureList() == null)
            return false;
        if (other.getStructureList() != null && other.getStructureList().equals(this.getStructureList()) == false)
            return false;
        if (other.getBlobList() == null ^ this.getBlobList() == null)
            return false;
        if (other.getBlobList() != null && other.getBlobList().equals(this.getBlobList()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getStringList() == null) ? 0 : getStringList().hashCode());
        hashCode = prime * hashCode + ((getStringSet() == null) ? 0 : getStringSet().hashCode());
        hashCode = prime * hashCode + ((getIntegerList() == null) ? 0 : getIntegerList().hashCode());
        hashCode = prime * hashCode + ((getBooleanList() == null) ? 0 : getBooleanList().hashCode());
        hashCode = prime * hashCode + ((getTimestampList() == null) ? 0 : getTimestampList().hashCode());
        hashCode = prime * hashCode + ((getEnumList() == null) ? 0 : getEnumList().hashCode());
        hashCode = prime * hashCode + ((getIntEnumList() == null) ? 0 : getIntEnumList().hashCode());
        hashCode = prime * hashCode + ((getNestedStringList() == null) ? 0 : getNestedStringList().hashCode());
        hashCode = prime * hashCode + ((getStructureList() == null) ? 0 : getStructureList().hashCode());
        hashCode = prime * hashCode + ((getBlobList() == null) ? 0 : getBlobList().hashCode());
        return hashCode;
    }

    @Override
    public RpcV2CborListsResult clone() {
        try {
            return (RpcV2CborListsResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
