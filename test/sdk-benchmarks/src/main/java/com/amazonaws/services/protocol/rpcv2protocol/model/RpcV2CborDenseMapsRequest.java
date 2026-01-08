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


import com.amazonaws.AmazonWebServiceRequest;

/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborDenseMaps" target="_top">AWS
 *      API Documentation</a>
 */

public class RpcV2CborDenseMapsRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private java.util.Map<String, GreetingStruct> denseStructMap;

    private java.util.Map<String, Integer> denseNumberMap;

    private java.util.Map<String, Boolean> denseBooleanMap;

    private java.util.Map<String, String> denseStringMap;

    private java.util.Map<String, java.util.List<String>> denseSetMap;

    /**
     * @return
     */

    public java.util.Map<String, GreetingStruct> getDenseStructMap() {
        return denseStructMap;
    }

    /**
     * @param denseStructMap
     */

    public void setDenseStructMap(java.util.Map<String, GreetingStruct> denseStructMap) {
        this.denseStructMap = denseStructMap;
    }

    /**
     * @param denseStructMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest withDenseStructMap(java.util.Map<String, GreetingStruct> denseStructMap) {
        setDenseStructMap(denseStructMap);
        return this;
    }

    /**
     * Add a single DenseStructMap entry
     *
     * @see RpcV2CborDenseMapsRequest#withDenseStructMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest addDenseStructMapEntry(String key, GreetingStruct value) {
        if (null == this.denseStructMap) {
            this.denseStructMap = new java.util.HashMap<String, GreetingStruct>();
        }
        if (this.denseStructMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.denseStructMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into DenseStructMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest clearDenseStructMapEntries() {
        this.denseStructMap = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, Integer> getDenseNumberMap() {
        return denseNumberMap;
    }

    /**
     * @param denseNumberMap
     */

    public void setDenseNumberMap(java.util.Map<String, Integer> denseNumberMap) {
        this.denseNumberMap = denseNumberMap;
    }

    /**
     * @param denseNumberMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest withDenseNumberMap(java.util.Map<String, Integer> denseNumberMap) {
        setDenseNumberMap(denseNumberMap);
        return this;
    }

    /**
     * Add a single DenseNumberMap entry
     *
     * @see RpcV2CborDenseMapsRequest#withDenseNumberMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest addDenseNumberMapEntry(String key, Integer value) {
        if (null == this.denseNumberMap) {
            this.denseNumberMap = new java.util.HashMap<String, Integer>();
        }
        if (this.denseNumberMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.denseNumberMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into DenseNumberMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest clearDenseNumberMapEntries() {
        this.denseNumberMap = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, Boolean> getDenseBooleanMap() {
        return denseBooleanMap;
    }

    /**
     * @param denseBooleanMap
     */

    public void setDenseBooleanMap(java.util.Map<String, Boolean> denseBooleanMap) {
        this.denseBooleanMap = denseBooleanMap;
    }

    /**
     * @param denseBooleanMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest withDenseBooleanMap(java.util.Map<String, Boolean> denseBooleanMap) {
        setDenseBooleanMap(denseBooleanMap);
        return this;
    }

    /**
     * Add a single DenseBooleanMap entry
     *
     * @see RpcV2CborDenseMapsRequest#withDenseBooleanMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest addDenseBooleanMapEntry(String key, Boolean value) {
        if (null == this.denseBooleanMap) {
            this.denseBooleanMap = new java.util.HashMap<String, Boolean>();
        }
        if (this.denseBooleanMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.denseBooleanMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into DenseBooleanMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest clearDenseBooleanMapEntries() {
        this.denseBooleanMap = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, String> getDenseStringMap() {
        return denseStringMap;
    }

    /**
     * @param denseStringMap
     */

    public void setDenseStringMap(java.util.Map<String, String> denseStringMap) {
        this.denseStringMap = denseStringMap;
    }

    /**
     * @param denseStringMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest withDenseStringMap(java.util.Map<String, String> denseStringMap) {
        setDenseStringMap(denseStringMap);
        return this;
    }

    /**
     * Add a single DenseStringMap entry
     *
     * @see RpcV2CborDenseMapsRequest#withDenseStringMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest addDenseStringMapEntry(String key, String value) {
        if (null == this.denseStringMap) {
            this.denseStringMap = new java.util.HashMap<String, String>();
        }
        if (this.denseStringMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.denseStringMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into DenseStringMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest clearDenseStringMapEntries() {
        this.denseStringMap = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, java.util.List<String>> getDenseSetMap() {
        return denseSetMap;
    }

    /**
     * @param denseSetMap
     */

    public void setDenseSetMap(java.util.Map<String, java.util.List<String>> denseSetMap) {
        this.denseSetMap = denseSetMap;
    }

    /**
     * @param denseSetMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest withDenseSetMap(java.util.Map<String, java.util.List<String>> denseSetMap) {
        setDenseSetMap(denseSetMap);
        return this;
    }

    /**
     * Add a single DenseSetMap entry
     *
     * @see RpcV2CborDenseMapsRequest#withDenseSetMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest addDenseSetMapEntry(String key, java.util.List<String> value) {
        if (null == this.denseSetMap) {
            this.denseSetMap = new java.util.HashMap<String, java.util.List<String>>();
        }
        if (this.denseSetMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.denseSetMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into DenseSetMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RpcV2CborDenseMapsRequest clearDenseSetMapEntries() {
        this.denseSetMap = null;
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
        if (getDenseStructMap() != null)
            sb.append("DenseStructMap: ").append(getDenseStructMap()).append(",");
        if (getDenseNumberMap() != null)
            sb.append("DenseNumberMap: ").append(getDenseNumberMap()).append(",");
        if (getDenseBooleanMap() != null)
            sb.append("DenseBooleanMap: ").append(getDenseBooleanMap()).append(",");
        if (getDenseStringMap() != null)
            sb.append("DenseStringMap: ").append(getDenseStringMap()).append(",");
        if (getDenseSetMap() != null)
            sb.append("DenseSetMap: ").append(getDenseSetMap());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof RpcV2CborDenseMapsRequest == false)
            return false;
        RpcV2CborDenseMapsRequest other = (RpcV2CborDenseMapsRequest) obj;
        if (other.getDenseStructMap() == null ^ this.getDenseStructMap() == null)
            return false;
        if (other.getDenseStructMap() != null && other.getDenseStructMap().equals(this.getDenseStructMap()) == false)
            return false;
        if (other.getDenseNumberMap() == null ^ this.getDenseNumberMap() == null)
            return false;
        if (other.getDenseNumberMap() != null && other.getDenseNumberMap().equals(this.getDenseNumberMap()) == false)
            return false;
        if (other.getDenseBooleanMap() == null ^ this.getDenseBooleanMap() == null)
            return false;
        if (other.getDenseBooleanMap() != null && other.getDenseBooleanMap().equals(this.getDenseBooleanMap()) == false)
            return false;
        if (other.getDenseStringMap() == null ^ this.getDenseStringMap() == null)
            return false;
        if (other.getDenseStringMap() != null && other.getDenseStringMap().equals(this.getDenseStringMap()) == false)
            return false;
        if (other.getDenseSetMap() == null ^ this.getDenseSetMap() == null)
            return false;
        if (other.getDenseSetMap() != null && other.getDenseSetMap().equals(this.getDenseSetMap()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getDenseStructMap() == null) ? 0 : getDenseStructMap().hashCode());
        hashCode = prime * hashCode + ((getDenseNumberMap() == null) ? 0 : getDenseNumberMap().hashCode());
        hashCode = prime * hashCode + ((getDenseBooleanMap() == null) ? 0 : getDenseBooleanMap().hashCode());
        hashCode = prime * hashCode + ((getDenseStringMap() == null) ? 0 : getDenseStringMap().hashCode());
        hashCode = prime * hashCode + ((getDenseSetMap() == null) ? 0 : getDenseSetMap().hashCode());
        return hashCode;
    }

    @Override
    public RpcV2CborDenseMapsRequest clone() {
        return (RpcV2CborDenseMapsRequest) super.clone();
    }

}
