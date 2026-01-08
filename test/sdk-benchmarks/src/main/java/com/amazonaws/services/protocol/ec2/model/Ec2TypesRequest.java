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
package com.amazonaws.services.protocol.ec2.model;

import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import java.io.Serializable;


import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.services.protocol.ec2.model.transform.Ec2TypesRequestMarshaller;

/**
 * 
 */

public class Ec2TypesRequest extends AmazonWebServiceRequest implements Serializable, Cloneable, DryRunSupportedRequest<Ec2TypesRequest> {

    private java.util.List<String> flattenedListOfStrings;

    private java.util.List<SimpleStruct> flattenedListOfStructs;

    private java.util.List<String> flattenedListWithLocation;

    private String stringMemberWithLocation;

    private String stringMemberWithQueryName;

    private String stringMemberWithLocationAndQueryName;

    private java.util.List<String> listMemberWithLocationAndQueryName;

    private java.util.List<String> listMemberWithOnlyMemberLocation;

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

    public Ec2TypesRequest withFlattenedListOfStrings(String... flattenedListOfStrings) {
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

    public Ec2TypesRequest withFlattenedListOfStrings(java.util.Collection<String> flattenedListOfStrings) {
        setFlattenedListOfStrings(flattenedListOfStrings);
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

    public Ec2TypesRequest withFlattenedListOfStructs(SimpleStruct... flattenedListOfStructs) {
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

    public Ec2TypesRequest withFlattenedListOfStructs(java.util.Collection<SimpleStruct> flattenedListOfStructs) {
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

    public Ec2TypesRequest withFlattenedListWithLocation(String... flattenedListWithLocation) {
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

    public Ec2TypesRequest withFlattenedListWithLocation(java.util.Collection<String> flattenedListWithLocation) {
        setFlattenedListWithLocation(flattenedListWithLocation);
        return this;
    }

    /**
     * @param stringMemberWithLocation
     */

    public void setStringMemberWithLocation(String stringMemberWithLocation) {
        this.stringMemberWithLocation = stringMemberWithLocation;
    }

    /**
     * @return
     */

    public String getStringMemberWithLocation() {
        return this.stringMemberWithLocation;
    }

    /**
     * @param stringMemberWithLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public Ec2TypesRequest withStringMemberWithLocation(String stringMemberWithLocation) {
        setStringMemberWithLocation(stringMemberWithLocation);
        return this;
    }

    /**
     * @param stringMemberWithQueryName
     */

    public void setStringMemberWithQueryName(String stringMemberWithQueryName) {
        this.stringMemberWithQueryName = stringMemberWithQueryName;
    }

    /**
     * @return
     */

    public String getStringMemberWithQueryName() {
        return this.stringMemberWithQueryName;
    }

    /**
     * @param stringMemberWithQueryName
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public Ec2TypesRequest withStringMemberWithQueryName(String stringMemberWithQueryName) {
        setStringMemberWithQueryName(stringMemberWithQueryName);
        return this;
    }

    /**
     * @param stringMemberWithLocationAndQueryName
     */

    public void setStringMemberWithLocationAndQueryName(String stringMemberWithLocationAndQueryName) {
        this.stringMemberWithLocationAndQueryName = stringMemberWithLocationAndQueryName;
    }

    /**
     * @return
     */

    public String getStringMemberWithLocationAndQueryName() {
        return this.stringMemberWithLocationAndQueryName;
    }

    /**
     * @param stringMemberWithLocationAndQueryName
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public Ec2TypesRequest withStringMemberWithLocationAndQueryName(String stringMemberWithLocationAndQueryName) {
        setStringMemberWithLocationAndQueryName(stringMemberWithLocationAndQueryName);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getListMemberWithLocationAndQueryName() {
        return listMemberWithLocationAndQueryName;
    }

    /**
     * @param listMemberWithLocationAndQueryName
     */

    public void setListMemberWithLocationAndQueryName(java.util.Collection<String> listMemberWithLocationAndQueryName) {
        if (listMemberWithLocationAndQueryName == null) {
            this.listMemberWithLocationAndQueryName = null;
            return;
        }

        this.listMemberWithLocationAndQueryName = new java.util.ArrayList<String>(listMemberWithLocationAndQueryName);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListMemberWithLocationAndQueryName(java.util.Collection)} or
     * {@link #withListMemberWithLocationAndQueryName(java.util.Collection)} if you want to override the existing
     * values.
     * </p>
     * 
     * @param listMemberWithLocationAndQueryName
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public Ec2TypesRequest withListMemberWithLocationAndQueryName(String... listMemberWithLocationAndQueryName) {
        if (this.listMemberWithLocationAndQueryName == null) {
            setListMemberWithLocationAndQueryName(new java.util.ArrayList<String>(listMemberWithLocationAndQueryName.length));
        }
        for (String ele : listMemberWithLocationAndQueryName) {
            this.listMemberWithLocationAndQueryName.add(ele);
        }
        return this;
    }

    /**
     * @param listMemberWithLocationAndQueryName
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public Ec2TypesRequest withListMemberWithLocationAndQueryName(java.util.Collection<String> listMemberWithLocationAndQueryName) {
        setListMemberWithLocationAndQueryName(listMemberWithLocationAndQueryName);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getListMemberWithOnlyMemberLocation() {
        return listMemberWithOnlyMemberLocation;
    }

    /**
     * @param listMemberWithOnlyMemberLocation
     */

    public void setListMemberWithOnlyMemberLocation(java.util.Collection<String> listMemberWithOnlyMemberLocation) {
        if (listMemberWithOnlyMemberLocation == null) {
            this.listMemberWithOnlyMemberLocation = null;
            return;
        }

        this.listMemberWithOnlyMemberLocation = new java.util.ArrayList<String>(listMemberWithOnlyMemberLocation);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListMemberWithOnlyMemberLocation(java.util.Collection)} or
     * {@link #withListMemberWithOnlyMemberLocation(java.util.Collection)} if you want to override the existing values.
     * </p>
     * 
     * @param listMemberWithOnlyMemberLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public Ec2TypesRequest withListMemberWithOnlyMemberLocation(String... listMemberWithOnlyMemberLocation) {
        if (this.listMemberWithOnlyMemberLocation == null) {
            setListMemberWithOnlyMemberLocation(new java.util.ArrayList<String>(listMemberWithOnlyMemberLocation.length));
        }
        for (String ele : listMemberWithOnlyMemberLocation) {
            this.listMemberWithOnlyMemberLocation.add(ele);
        }
        return this;
    }

    /**
     * @param listMemberWithOnlyMemberLocation
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public Ec2TypesRequest withListMemberWithOnlyMemberLocation(java.util.Collection<String> listMemberWithOnlyMemberLocation) {
        setListMemberWithOnlyMemberLocation(listMemberWithOnlyMemberLocation);
        return this;
    }

    /**
     * This method is intended for internal use only. Returns the marshaled request configured with additional
     * parameters to enable operation dry-run.
     */
    @Override
    public Request<Ec2TypesRequest> getDryRunRequest() {
        Request<Ec2TypesRequest> request = new Ec2TypesRequestMarshaller().marshall(this);
        request.addParameter("DryRun", Boolean.toString(true));
        return request;
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
        if (getFlattenedListOfStructs() != null)
            sb.append("FlattenedListOfStructs: ").append(getFlattenedListOfStructs()).append(",");
        if (getFlattenedListWithLocation() != null)
            sb.append("FlattenedListWithLocation: ").append(getFlattenedListWithLocation()).append(",");
        if (getStringMemberWithLocation() != null)
            sb.append("StringMemberWithLocation: ").append(getStringMemberWithLocation()).append(",");
        if (getStringMemberWithQueryName() != null)
            sb.append("StringMemberWithQueryName: ").append(getStringMemberWithQueryName()).append(",");
        if (getStringMemberWithLocationAndQueryName() != null)
            sb.append("StringMemberWithLocationAndQueryName: ").append(getStringMemberWithLocationAndQueryName()).append(",");
        if (getListMemberWithLocationAndQueryName() != null)
            sb.append("ListMemberWithLocationAndQueryName: ").append(getListMemberWithLocationAndQueryName()).append(",");
        if (getListMemberWithOnlyMemberLocation() != null)
            sb.append("ListMemberWithOnlyMemberLocation: ").append(getListMemberWithOnlyMemberLocation());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof Ec2TypesRequest == false)
            return false;
        Ec2TypesRequest other = (Ec2TypesRequest) obj;
        if (other.getFlattenedListOfStrings() == null ^ this.getFlattenedListOfStrings() == null)
            return false;
        if (other.getFlattenedListOfStrings() != null && other.getFlattenedListOfStrings().equals(this.getFlattenedListOfStrings()) == false)
            return false;
        if (other.getFlattenedListOfStructs() == null ^ this.getFlattenedListOfStructs() == null)
            return false;
        if (other.getFlattenedListOfStructs() != null && other.getFlattenedListOfStructs().equals(this.getFlattenedListOfStructs()) == false)
            return false;
        if (other.getFlattenedListWithLocation() == null ^ this.getFlattenedListWithLocation() == null)
            return false;
        if (other.getFlattenedListWithLocation() != null && other.getFlattenedListWithLocation().equals(this.getFlattenedListWithLocation()) == false)
            return false;
        if (other.getStringMemberWithLocation() == null ^ this.getStringMemberWithLocation() == null)
            return false;
        if (other.getStringMemberWithLocation() != null && other.getStringMemberWithLocation().equals(this.getStringMemberWithLocation()) == false)
            return false;
        if (other.getStringMemberWithQueryName() == null ^ this.getStringMemberWithQueryName() == null)
            return false;
        if (other.getStringMemberWithQueryName() != null && other.getStringMemberWithQueryName().equals(this.getStringMemberWithQueryName()) == false)
            return false;
        if (other.getStringMemberWithLocationAndQueryName() == null ^ this.getStringMemberWithLocationAndQueryName() == null)
            return false;
        if (other.getStringMemberWithLocationAndQueryName() != null
                && other.getStringMemberWithLocationAndQueryName().equals(this.getStringMemberWithLocationAndQueryName()) == false)
            return false;
        if (other.getListMemberWithLocationAndQueryName() == null ^ this.getListMemberWithLocationAndQueryName() == null)
            return false;
        if (other.getListMemberWithLocationAndQueryName() != null
                && other.getListMemberWithLocationAndQueryName().equals(this.getListMemberWithLocationAndQueryName()) == false)
            return false;
        if (other.getListMemberWithOnlyMemberLocation() == null ^ this.getListMemberWithOnlyMemberLocation() == null)
            return false;
        if (other.getListMemberWithOnlyMemberLocation() != null
                && other.getListMemberWithOnlyMemberLocation().equals(this.getListMemberWithOnlyMemberLocation()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getFlattenedListOfStrings() == null) ? 0 : getFlattenedListOfStrings().hashCode());
        hashCode = prime * hashCode + ((getFlattenedListOfStructs() == null) ? 0 : getFlattenedListOfStructs().hashCode());
        hashCode = prime * hashCode + ((getFlattenedListWithLocation() == null) ? 0 : getFlattenedListWithLocation().hashCode());
        hashCode = prime * hashCode + ((getStringMemberWithLocation() == null) ? 0 : getStringMemberWithLocation().hashCode());
        hashCode = prime * hashCode + ((getStringMemberWithQueryName() == null) ? 0 : getStringMemberWithQueryName().hashCode());
        hashCode = prime * hashCode + ((getStringMemberWithLocationAndQueryName() == null) ? 0 : getStringMemberWithLocationAndQueryName().hashCode());
        hashCode = prime * hashCode + ((getListMemberWithLocationAndQueryName() == null) ? 0 : getListMemberWithLocationAndQueryName().hashCode());
        hashCode = prime * hashCode + ((getListMemberWithOnlyMemberLocation() == null) ? 0 : getListMemberWithOnlyMemberLocation().hashCode());
        return hashCode;
    }

    @Override
    public Ec2TypesRequest clone() {
        return (Ec2TypesRequest) super.clone();
    }
}
