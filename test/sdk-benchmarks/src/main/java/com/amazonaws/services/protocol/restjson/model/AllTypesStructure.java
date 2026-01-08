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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/AllTypesStructure" target="_top">AWS API
 *      Documentation</a>
 */

public class AllTypesStructure implements Serializable, Cloneable, StructuredPojo {

    private String stringMember;

    private Integer integerMember;

    private Boolean booleanMember;

    private Float floatMember;

    private Double doubleMember;

    private Long longMember;

    private com.amazonaws.internal.SdkInternalList<String> simpleList;

    private com.amazonaws.internal.SdkInternalList<java.util.Map<String, String>> listOfMaps;

    private com.amazonaws.internal.SdkInternalList<SimpleStruct> listOfStructs;

    private com.amazonaws.internal.SdkInternalMap<String, java.util.List<Integer>> mapOfStringToIntegerList;

    private com.amazonaws.internal.SdkInternalMap<String, String> mapOfStringToString;

    private com.amazonaws.internal.SdkInternalMap<String, SimpleStruct> mapOfStringToStruct;

    private java.util.Date timestampMember;

    private StructWithTimestamp structWithNestedTimestampMember;

    private java.nio.ByteBuffer blobArg;

    private StructWithNestedBlobType structWithNestedBlob;

    private com.amazonaws.internal.SdkInternalMap<String, java.nio.ByteBuffer> blobMap;

    private com.amazonaws.internal.SdkInternalList<java.nio.ByteBuffer> listOfBlobs;

    private RecursiveStructType recursiveStruct;

    private BaseType polymorphicTypeWithSubTypes;

    private SubTypeOne polymorphicTypeWithoutSubTypes;

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

    public AllTypesStructure withStringMember(String stringMember) {
        setStringMember(stringMember);
        return this;
    }

    /**
     * @param integerMember
     */

    public void setIntegerMember(Integer integerMember) {
        this.integerMember = integerMember;
    }

    /**
     * @return
     */

    public Integer getIntegerMember() {
        return this.integerMember;
    }

    /**
     * @param integerMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withIntegerMember(Integer integerMember) {
        setIntegerMember(integerMember);
        return this;
    }

    /**
     * @param booleanMember
     */

    public void setBooleanMember(Boolean booleanMember) {
        this.booleanMember = booleanMember;
    }

    /**
     * @return
     */

    public Boolean getBooleanMember() {
        return this.booleanMember;
    }

    /**
     * @param booleanMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withBooleanMember(Boolean booleanMember) {
        setBooleanMember(booleanMember);
        return this;
    }

    /**
     * @return
     */

    public Boolean isBooleanMember() {
        return this.booleanMember;
    }

    /**
     * @param floatMember
     */

    public void setFloatMember(Float floatMember) {
        this.floatMember = floatMember;
    }

    /**
     * @return
     */

    public Float getFloatMember() {
        return this.floatMember;
    }

    /**
     * @param floatMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withFloatMember(Float floatMember) {
        setFloatMember(floatMember);
        return this;
    }

    /**
     * @param doubleMember
     */

    public void setDoubleMember(Double doubleMember) {
        this.doubleMember = doubleMember;
    }

    /**
     * @return
     */

    public Double getDoubleMember() {
        return this.doubleMember;
    }

    /**
     * @param doubleMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withDoubleMember(Double doubleMember) {
        setDoubleMember(doubleMember);
        return this;
    }

    /**
     * @param longMember
     */

    public void setLongMember(Long longMember) {
        this.longMember = longMember;
    }

    /**
     * @return
     */

    public Long getLongMember() {
        return this.longMember;
    }

    /**
     * @param longMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withLongMember(Long longMember) {
        setLongMember(longMember);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getSimpleList() {
        if (simpleList == null) {
            simpleList = new com.amazonaws.internal.SdkInternalList<String>();
        }
        return simpleList;
    }

    /**
     * @param simpleList
     */

    public void setSimpleList(java.util.Collection<String> simpleList) {
        if (simpleList == null) {
            this.simpleList = null;
            return;
        }

        this.simpleList = new com.amazonaws.internal.SdkInternalList<String>(simpleList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setSimpleList(java.util.Collection)} or {@link #withSimpleList(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param simpleList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withSimpleList(String... simpleList) {
        if (this.simpleList == null) {
            setSimpleList(new com.amazonaws.internal.SdkInternalList<String>(simpleList.length));
        }
        for (String ele : simpleList) {
            this.simpleList.add(ele);
        }
        return this;
    }

    /**
     * @param simpleList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withSimpleList(java.util.Collection<String> simpleList) {
        setSimpleList(simpleList);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.util.Map<String, String>> getListOfMaps() {
        if (listOfMaps == null) {
            listOfMaps = new com.amazonaws.internal.SdkInternalList<java.util.Map<String, String>>();
        }
        return listOfMaps;
    }

    /**
     * @param listOfMaps
     */

    public void setListOfMaps(java.util.Collection<java.util.Map<String, String>> listOfMaps) {
        if (listOfMaps == null) {
            this.listOfMaps = null;
            return;
        }

        this.listOfMaps = new com.amazonaws.internal.SdkInternalList<java.util.Map<String, String>>(listOfMaps);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfMaps(java.util.Collection)} or {@link #withListOfMaps(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param listOfMaps
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withListOfMaps(java.util.Map<String, String>... listOfMaps) {
        if (this.listOfMaps == null) {
            setListOfMaps(new com.amazonaws.internal.SdkInternalList<java.util.Map<String, String>>(listOfMaps.length));
        }
        for (java.util.Map<String, String> ele : listOfMaps) {
            this.listOfMaps.add(ele);
        }
        return this;
    }

    /**
     * @param listOfMaps
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withListOfMaps(java.util.Collection<java.util.Map<String, String>> listOfMaps) {
        setListOfMaps(listOfMaps);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<SimpleStruct> getListOfStructs() {
        if (listOfStructs == null) {
            listOfStructs = new com.amazonaws.internal.SdkInternalList<SimpleStruct>();
        }
        return listOfStructs;
    }

    /**
     * @param listOfStructs
     */

    public void setListOfStructs(java.util.Collection<SimpleStruct> listOfStructs) {
        if (listOfStructs == null) {
            this.listOfStructs = null;
            return;
        }

        this.listOfStructs = new com.amazonaws.internal.SdkInternalList<SimpleStruct>(listOfStructs);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfStructs(java.util.Collection)} or {@link #withListOfStructs(java.util.Collection)} if you want
     * to override the existing values.
     * </p>
     * 
     * @param listOfStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withListOfStructs(SimpleStruct... listOfStructs) {
        if (this.listOfStructs == null) {
            setListOfStructs(new com.amazonaws.internal.SdkInternalList<SimpleStruct>(listOfStructs.length));
        }
        for (SimpleStruct ele : listOfStructs) {
            this.listOfStructs.add(ele);
        }
        return this;
    }

    /**
     * @param listOfStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withListOfStructs(java.util.Collection<SimpleStruct> listOfStructs) {
        setListOfStructs(listOfStructs);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, java.util.List<Integer>> getMapOfStringToIntegerList() {
        if (mapOfStringToIntegerList == null) {
            mapOfStringToIntegerList = new com.amazonaws.internal.SdkInternalMap<String, java.util.List<Integer>>();
        }
        return mapOfStringToIntegerList;
    }

    /**
     * @param mapOfStringToIntegerList
     */

    public void setMapOfStringToIntegerList(java.util.Map<String, java.util.List<Integer>> mapOfStringToIntegerList) {
        this.mapOfStringToIntegerList = mapOfStringToIntegerList == null ? null : new com.amazonaws.internal.SdkInternalMap<String, java.util.List<Integer>>(
                mapOfStringToIntegerList);
    }

    /**
     * @param mapOfStringToIntegerList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withMapOfStringToIntegerList(java.util.Map<String, java.util.List<Integer>> mapOfStringToIntegerList) {
        setMapOfStringToIntegerList(mapOfStringToIntegerList);
        return this;
    }

    /**
     * Add a single MapOfStringToIntegerList entry
     *
     * @see AllTypesStructure#withMapOfStringToIntegerList
     * @returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure addMapOfStringToIntegerListEntry(String key, java.util.List<Integer> value) {
        if (null == this.mapOfStringToIntegerList) {
            this.mapOfStringToIntegerList = new com.amazonaws.internal.SdkInternalMap<String, java.util.List<Integer>>();
        }
        if (this.mapOfStringToIntegerList.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.mapOfStringToIntegerList.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into MapOfStringToIntegerList.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure clearMapOfStringToIntegerListEntries() {
        this.mapOfStringToIntegerList = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, String> getMapOfStringToString() {
        if (mapOfStringToString == null) {
            mapOfStringToString = new com.amazonaws.internal.SdkInternalMap<String, String>();
        }
        return mapOfStringToString;
    }

    /**
     * @param mapOfStringToString
     */

    public void setMapOfStringToString(java.util.Map<String, String> mapOfStringToString) {
        this.mapOfStringToString = mapOfStringToString == null ? null : new com.amazonaws.internal.SdkInternalMap<String, String>(mapOfStringToString);
    }

    /**
     * @param mapOfStringToString
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withMapOfStringToString(java.util.Map<String, String> mapOfStringToString) {
        setMapOfStringToString(mapOfStringToString);
        return this;
    }

    /**
     * Add a single MapOfStringToString entry
     *
     * @see AllTypesStructure#withMapOfStringToString
     * @returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure addMapOfStringToStringEntry(String key, String value) {
        if (null == this.mapOfStringToString) {
            this.mapOfStringToString = new com.amazonaws.internal.SdkInternalMap<String, String>();
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

    public AllTypesStructure clearMapOfStringToStringEntries() {
        this.mapOfStringToString = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, SimpleStruct> getMapOfStringToStruct() {
        if (mapOfStringToStruct == null) {
            mapOfStringToStruct = new com.amazonaws.internal.SdkInternalMap<String, SimpleStruct>();
        }
        return mapOfStringToStruct;
    }

    /**
     * @param mapOfStringToStruct
     */

    public void setMapOfStringToStruct(java.util.Map<String, SimpleStruct> mapOfStringToStruct) {
        this.mapOfStringToStruct = mapOfStringToStruct == null ? null : new com.amazonaws.internal.SdkInternalMap<String, SimpleStruct>(mapOfStringToStruct);
    }

    /**
     * @param mapOfStringToStruct
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withMapOfStringToStruct(java.util.Map<String, SimpleStruct> mapOfStringToStruct) {
        setMapOfStringToStruct(mapOfStringToStruct);
        return this;
    }

    /**
     * Add a single MapOfStringToStruct entry
     *
     * @see AllTypesStructure#withMapOfStringToStruct
     * @returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure addMapOfStringToStructEntry(String key, SimpleStruct value) {
        if (null == this.mapOfStringToStruct) {
            this.mapOfStringToStruct = new com.amazonaws.internal.SdkInternalMap<String, SimpleStruct>();
        }
        if (this.mapOfStringToStruct.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.mapOfStringToStruct.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into MapOfStringToStruct.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure clearMapOfStringToStructEntries() {
        this.mapOfStringToStruct = null;
        return this;
    }

    /**
     * @param timestampMember
     */

    public void setTimestampMember(java.util.Date timestampMember) {
        this.timestampMember = timestampMember;
    }

    /**
     * @return
     */

    public java.util.Date getTimestampMember() {
        return this.timestampMember;
    }

    /**
     * @param timestampMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withTimestampMember(java.util.Date timestampMember) {
        setTimestampMember(timestampMember);
        return this;
    }

    /**
     * @param structWithNestedTimestampMember
     */

    public void setStructWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
        this.structWithNestedTimestampMember = structWithNestedTimestampMember;
    }

    /**
     * @return
     */

    public StructWithTimestamp getStructWithNestedTimestampMember() {
        return this.structWithNestedTimestampMember;
    }

    /**
     * @param structWithNestedTimestampMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withStructWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
        setStructWithNestedTimestampMember(structWithNestedTimestampMember);
        return this;
    }

    /**
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * <p>
     * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content or position of the byte buffer will
     * be seen by all objects that have a reference to this object. It is recommended to call ByteBuffer.duplicate() or
     * ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer. This behavior will be changed in a future
     * major version of the SDK.
     * </p>
     * 
     * @param blobArg
     */

    public void setBlobArg(java.nio.ByteBuffer blobArg) {
        this.blobArg = blobArg;
    }

    /**
     * <p>
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}.
     * </p>
     * 
     * @return
     */

    public java.nio.ByteBuffer getBlobArg() {
        return this.blobArg;
    }

    /**
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * <p>
     * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content or position of the byte buffer will
     * be seen by all objects that have a reference to this object. It is recommended to call ByteBuffer.duplicate() or
     * ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer. This behavior will be changed in a future
     * major version of the SDK.
     * </p>
     * 
     * @param blobArg
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withBlobArg(java.nio.ByteBuffer blobArg) {
        setBlobArg(blobArg);
        return this;
    }

    /**
     * @param structWithNestedBlob
     */

    public void setStructWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
        this.structWithNestedBlob = structWithNestedBlob;
    }

    /**
     * @return
     */

    public StructWithNestedBlobType getStructWithNestedBlob() {
        return this.structWithNestedBlob;
    }

    /**
     * @param structWithNestedBlob
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withStructWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
        setStructWithNestedBlob(structWithNestedBlob);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, java.nio.ByteBuffer> getBlobMap() {
        if (blobMap == null) {
            blobMap = new com.amazonaws.internal.SdkInternalMap<String, java.nio.ByteBuffer>();
        }
        return blobMap;
    }

    /**
     * @param blobMap
     */

    public void setBlobMap(java.util.Map<String, java.nio.ByteBuffer> blobMap) {
        this.blobMap = blobMap == null ? null : new com.amazonaws.internal.SdkInternalMap<String, java.nio.ByteBuffer>(blobMap);
    }

    /**
     * @param blobMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withBlobMap(java.util.Map<String, java.nio.ByteBuffer> blobMap) {
        setBlobMap(blobMap);
        return this;
    }

    /**
     * Add a single BlobMap entry
     *
     * @see AllTypesStructure#withBlobMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure addBlobMapEntry(String key, java.nio.ByteBuffer value) {
        if (null == this.blobMap) {
            this.blobMap = new com.amazonaws.internal.SdkInternalMap<String, java.nio.ByteBuffer>();
        }
        if (this.blobMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.blobMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into BlobMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure clearBlobMapEntries() {
        this.blobMap = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.nio.ByteBuffer> getListOfBlobs() {
        if (listOfBlobs == null) {
            listOfBlobs = new com.amazonaws.internal.SdkInternalList<java.nio.ByteBuffer>();
        }
        return listOfBlobs;
    }

    /**
     * @param listOfBlobs
     */

    public void setListOfBlobs(java.util.Collection<java.nio.ByteBuffer> listOfBlobs) {
        if (listOfBlobs == null) {
            this.listOfBlobs = null;
            return;
        }

        this.listOfBlobs = new com.amazonaws.internal.SdkInternalList<java.nio.ByteBuffer>(listOfBlobs);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfBlobs(java.util.Collection)} or {@link #withListOfBlobs(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param listOfBlobs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withListOfBlobs(java.nio.ByteBuffer... listOfBlobs) {
        if (this.listOfBlobs == null) {
            setListOfBlobs(new com.amazonaws.internal.SdkInternalList<java.nio.ByteBuffer>(listOfBlobs.length));
        }
        for (java.nio.ByteBuffer ele : listOfBlobs) {
            this.listOfBlobs.add(ele);
        }
        return this;
    }

    /**
     * @param listOfBlobs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withListOfBlobs(java.util.Collection<java.nio.ByteBuffer> listOfBlobs) {
        setListOfBlobs(listOfBlobs);
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

    public AllTypesStructure withRecursiveStruct(RecursiveStructType recursiveStruct) {
        setRecursiveStruct(recursiveStruct);
        return this;
    }

    /**
     * @param polymorphicTypeWithSubTypes
     */

    public void setPolymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
        this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
    }

    /**
     * @return
     */

    public BaseType getPolymorphicTypeWithSubTypes() {
        return this.polymorphicTypeWithSubTypes;
    }

    /**
     * @param polymorphicTypeWithSubTypes
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withPolymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
        setPolymorphicTypeWithSubTypes(polymorphicTypeWithSubTypes);
        return this;
    }

    /**
     * @param polymorphicTypeWithoutSubTypes
     */

    public void setPolymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
        this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
    }

    /**
     * @return
     */

    public SubTypeOne getPolymorphicTypeWithoutSubTypes() {
        return this.polymorphicTypeWithoutSubTypes;
    }

    /**
     * @param polymorphicTypeWithoutSubTypes
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesStructure withPolymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
        setPolymorphicTypeWithoutSubTypes(polymorphicTypeWithoutSubTypes);
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
        if (getStringMember() != null)
            sb.append("StringMember: ").append(getStringMember()).append(",");
        if (getIntegerMember() != null)
            sb.append("IntegerMember: ").append(getIntegerMember()).append(",");
        if (getBooleanMember() != null)
            sb.append("BooleanMember: ").append(getBooleanMember()).append(",");
        if (getFloatMember() != null)
            sb.append("FloatMember: ").append(getFloatMember()).append(",");
        if (getDoubleMember() != null)
            sb.append("DoubleMember: ").append(getDoubleMember()).append(",");
        if (getLongMember() != null)
            sb.append("LongMember: ").append(getLongMember()).append(",");
        if (getSimpleList() != null)
            sb.append("SimpleList: ").append(getSimpleList()).append(",");
        if (getListOfMaps() != null)
            sb.append("ListOfMaps: ").append(getListOfMaps()).append(",");
        if (getListOfStructs() != null)
            sb.append("ListOfStructs: ").append(getListOfStructs()).append(",");
        if (getMapOfStringToIntegerList() != null)
            sb.append("MapOfStringToIntegerList: ").append(getMapOfStringToIntegerList()).append(",");
        if (getMapOfStringToString() != null)
            sb.append("MapOfStringToString: ").append(getMapOfStringToString()).append(",");
        if (getMapOfStringToStruct() != null)
            sb.append("MapOfStringToStruct: ").append(getMapOfStringToStruct()).append(",");
        if (getTimestampMember() != null)
            sb.append("TimestampMember: ").append(getTimestampMember()).append(",");
        if (getStructWithNestedTimestampMember() != null)
            sb.append("StructWithNestedTimestampMember: ").append(getStructWithNestedTimestampMember()).append(",");
        if (getBlobArg() != null)
            sb.append("BlobArg: ").append(getBlobArg()).append(",");
        if (getStructWithNestedBlob() != null)
            sb.append("StructWithNestedBlob: ").append(getStructWithNestedBlob()).append(",");
        if (getBlobMap() != null)
            sb.append("BlobMap: ").append(getBlobMap()).append(",");
        if (getListOfBlobs() != null)
            sb.append("ListOfBlobs: ").append(getListOfBlobs()).append(",");
        if (getRecursiveStruct() != null)
            sb.append("RecursiveStruct: ").append(getRecursiveStruct()).append(",");
        if (getPolymorphicTypeWithSubTypes() != null)
            sb.append("PolymorphicTypeWithSubTypes: ").append(getPolymorphicTypeWithSubTypes()).append(",");
        if (getPolymorphicTypeWithoutSubTypes() != null)
            sb.append("PolymorphicTypeWithoutSubTypes: ").append(getPolymorphicTypeWithoutSubTypes());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof AllTypesStructure == false)
            return false;
        AllTypesStructure other = (AllTypesStructure) obj;
        if (other.getStringMember() == null ^ this.getStringMember() == null)
            return false;
        if (other.getStringMember() != null && other.getStringMember().equals(this.getStringMember()) == false)
            return false;
        if (other.getIntegerMember() == null ^ this.getIntegerMember() == null)
            return false;
        if (other.getIntegerMember() != null && other.getIntegerMember().equals(this.getIntegerMember()) == false)
            return false;
        if (other.getBooleanMember() == null ^ this.getBooleanMember() == null)
            return false;
        if (other.getBooleanMember() != null && other.getBooleanMember().equals(this.getBooleanMember()) == false)
            return false;
        if (other.getFloatMember() == null ^ this.getFloatMember() == null)
            return false;
        if (other.getFloatMember() != null && other.getFloatMember().equals(this.getFloatMember()) == false)
            return false;
        if (other.getDoubleMember() == null ^ this.getDoubleMember() == null)
            return false;
        if (other.getDoubleMember() != null && other.getDoubleMember().equals(this.getDoubleMember()) == false)
            return false;
        if (other.getLongMember() == null ^ this.getLongMember() == null)
            return false;
        if (other.getLongMember() != null && other.getLongMember().equals(this.getLongMember()) == false)
            return false;
        if (other.getSimpleList() == null ^ this.getSimpleList() == null)
            return false;
        if (other.getSimpleList() != null && other.getSimpleList().equals(this.getSimpleList()) == false)
            return false;
        if (other.getListOfMaps() == null ^ this.getListOfMaps() == null)
            return false;
        if (other.getListOfMaps() != null && other.getListOfMaps().equals(this.getListOfMaps()) == false)
            return false;
        if (other.getListOfStructs() == null ^ this.getListOfStructs() == null)
            return false;
        if (other.getListOfStructs() != null && other.getListOfStructs().equals(this.getListOfStructs()) == false)
            return false;
        if (other.getMapOfStringToIntegerList() == null ^ this.getMapOfStringToIntegerList() == null)
            return false;
        if (other.getMapOfStringToIntegerList() != null && other.getMapOfStringToIntegerList().equals(this.getMapOfStringToIntegerList()) == false)
            return false;
        if (other.getMapOfStringToString() == null ^ this.getMapOfStringToString() == null)
            return false;
        if (other.getMapOfStringToString() != null && other.getMapOfStringToString().equals(this.getMapOfStringToString()) == false)
            return false;
        if (other.getMapOfStringToStruct() == null ^ this.getMapOfStringToStruct() == null)
            return false;
        if (other.getMapOfStringToStruct() != null && other.getMapOfStringToStruct().equals(this.getMapOfStringToStruct()) == false)
            return false;
        if (other.getTimestampMember() == null ^ this.getTimestampMember() == null)
            return false;
        if (other.getTimestampMember() != null && other.getTimestampMember().equals(this.getTimestampMember()) == false)
            return false;
        if (other.getStructWithNestedTimestampMember() == null ^ this.getStructWithNestedTimestampMember() == null)
            return false;
        if (other.getStructWithNestedTimestampMember() != null
                && other.getStructWithNestedTimestampMember().equals(this.getStructWithNestedTimestampMember()) == false)
            return false;
        if (other.getBlobArg() == null ^ this.getBlobArg() == null)
            return false;
        if (other.getBlobArg() != null && other.getBlobArg().equals(this.getBlobArg()) == false)
            return false;
        if (other.getStructWithNestedBlob() == null ^ this.getStructWithNestedBlob() == null)
            return false;
        if (other.getStructWithNestedBlob() != null && other.getStructWithNestedBlob().equals(this.getStructWithNestedBlob()) == false)
            return false;
        if (other.getBlobMap() == null ^ this.getBlobMap() == null)
            return false;
        if (other.getBlobMap() != null && other.getBlobMap().equals(this.getBlobMap()) == false)
            return false;
        if (other.getListOfBlobs() == null ^ this.getListOfBlobs() == null)
            return false;
        if (other.getListOfBlobs() != null && other.getListOfBlobs().equals(this.getListOfBlobs()) == false)
            return false;
        if (other.getRecursiveStruct() == null ^ this.getRecursiveStruct() == null)
            return false;
        if (other.getRecursiveStruct() != null && other.getRecursiveStruct().equals(this.getRecursiveStruct()) == false)
            return false;
        if (other.getPolymorphicTypeWithSubTypes() == null ^ this.getPolymorphicTypeWithSubTypes() == null)
            return false;
        if (other.getPolymorphicTypeWithSubTypes() != null && other.getPolymorphicTypeWithSubTypes().equals(this.getPolymorphicTypeWithSubTypes()) == false)
            return false;
        if (other.getPolymorphicTypeWithoutSubTypes() == null ^ this.getPolymorphicTypeWithoutSubTypes() == null)
            return false;
        if (other.getPolymorphicTypeWithoutSubTypes() != null
                && other.getPolymorphicTypeWithoutSubTypes().equals(this.getPolymorphicTypeWithoutSubTypes()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getStringMember() == null) ? 0 : getStringMember().hashCode());
        hashCode = prime * hashCode + ((getIntegerMember() == null) ? 0 : getIntegerMember().hashCode());
        hashCode = prime * hashCode + ((getBooleanMember() == null) ? 0 : getBooleanMember().hashCode());
        hashCode = prime * hashCode + ((getFloatMember() == null) ? 0 : getFloatMember().hashCode());
        hashCode = prime * hashCode + ((getDoubleMember() == null) ? 0 : getDoubleMember().hashCode());
        hashCode = prime * hashCode + ((getLongMember() == null) ? 0 : getLongMember().hashCode());
        hashCode = prime * hashCode + ((getSimpleList() == null) ? 0 : getSimpleList().hashCode());
        hashCode = prime * hashCode + ((getListOfMaps() == null) ? 0 : getListOfMaps().hashCode());
        hashCode = prime * hashCode + ((getListOfStructs() == null) ? 0 : getListOfStructs().hashCode());
        hashCode = prime * hashCode + ((getMapOfStringToIntegerList() == null) ? 0 : getMapOfStringToIntegerList().hashCode());
        hashCode = prime * hashCode + ((getMapOfStringToString() == null) ? 0 : getMapOfStringToString().hashCode());
        hashCode = prime * hashCode + ((getMapOfStringToStruct() == null) ? 0 : getMapOfStringToStruct().hashCode());
        hashCode = prime * hashCode + ((getTimestampMember() == null) ? 0 : getTimestampMember().hashCode());
        hashCode = prime * hashCode + ((getStructWithNestedTimestampMember() == null) ? 0 : getStructWithNestedTimestampMember().hashCode());
        hashCode = prime * hashCode + ((getBlobArg() == null) ? 0 : getBlobArg().hashCode());
        hashCode = prime * hashCode + ((getStructWithNestedBlob() == null) ? 0 : getStructWithNestedBlob().hashCode());
        hashCode = prime * hashCode + ((getBlobMap() == null) ? 0 : getBlobMap().hashCode());
        hashCode = prime * hashCode + ((getListOfBlobs() == null) ? 0 : getListOfBlobs().hashCode());
        hashCode = prime * hashCode + ((getRecursiveStruct() == null) ? 0 : getRecursiveStruct().hashCode());
        hashCode = prime * hashCode + ((getPolymorphicTypeWithSubTypes() == null) ? 0 : getPolymorphicTypeWithSubTypes().hashCode());
        hashCode = prime * hashCode + ((getPolymorphicTypeWithoutSubTypes() == null) ? 0 : getPolymorphicTypeWithoutSubTypes().hashCode());
        return hashCode;
    }

    @Override
    public AllTypesStructure clone() {
        try {
            return (AllTypesStructure) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.restjson.model.transform.AllTypesStructureMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
