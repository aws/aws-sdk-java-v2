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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/FurtherNestedContainers" target="_top">AWS
 *      API Documentation</a>
 */

public class FurtherNestedContainersRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private com.amazonaws.internal.SdkInternalList<NestedContainersStructure> listOfNested;

    /**
     * @return
     */

    public java.util.List<NestedContainersStructure> getListOfNested() {
        if (listOfNested == null) {
            listOfNested = new com.amazonaws.internal.SdkInternalList<NestedContainersStructure>();
        }
        return listOfNested;
    }

    /**
     * @param listOfNested
     */

    public void setListOfNested(java.util.Collection<NestedContainersStructure> listOfNested) {
        if (listOfNested == null) {
            this.listOfNested = null;
            return;
        }

        this.listOfNested = new com.amazonaws.internal.SdkInternalList<NestedContainersStructure>(listOfNested);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfNested(java.util.Collection)} or {@link #withListOfNested(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param listOfNested
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public FurtherNestedContainersRequest withListOfNested(NestedContainersStructure... listOfNested) {
        if (this.listOfNested == null) {
            setListOfNested(new com.amazonaws.internal.SdkInternalList<NestedContainersStructure>(listOfNested.length));
        }
        for (NestedContainersStructure ele : listOfNested) {
            this.listOfNested.add(ele);
        }
        return this;
    }

    /**
     * @param listOfNested
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public FurtherNestedContainersRequest withListOfNested(java.util.Collection<NestedContainersStructure> listOfNested) {
        setListOfNested(listOfNested);
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
        if (getListOfNested() != null)
            sb.append("ListOfNested: ").append(getListOfNested());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof FurtherNestedContainersRequest == false)
            return false;
        FurtherNestedContainersRequest other = (FurtherNestedContainersRequest) obj;
        if (other.getListOfNested() == null ^ this.getListOfNested() == null)
            return false;
        if (other.getListOfNested() != null && other.getListOfNested().equals(this.getListOfNested()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getListOfNested() == null) ? 0 : getListOfNested().hashCode());
        return hashCode;
    }

    @Override
    public FurtherNestedContainersRequest clone() {
        return (FurtherNestedContainersRequest) super.clone();
    }

}
