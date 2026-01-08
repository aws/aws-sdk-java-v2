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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithGreedyLabel" target="_top">AWS
 *      API Documentation</a>
 */

public class OperationWithGreedyLabelRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private String nonGreedyPathParam;

    private String greedyPathParam;

    /**
     * @param nonGreedyPathParam
     */

    public void setNonGreedyPathParam(String nonGreedyPathParam) {
        this.nonGreedyPathParam = nonGreedyPathParam;
    }

    /**
     * @return
     */

    public String getNonGreedyPathParam() {
        return this.nonGreedyPathParam;
    }

    /**
     * @param nonGreedyPathParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public OperationWithGreedyLabelRequest withNonGreedyPathParam(String nonGreedyPathParam) {
        setNonGreedyPathParam(nonGreedyPathParam);
        return this;
    }

    /**
     * @param greedyPathParam
     */

    public void setGreedyPathParam(String greedyPathParam) {
        this.greedyPathParam = greedyPathParam;
    }

    /**
     * @return
     */

    public String getGreedyPathParam() {
        return this.greedyPathParam;
    }

    /**
     * @param greedyPathParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public OperationWithGreedyLabelRequest withGreedyPathParam(String greedyPathParam) {
        setGreedyPathParam(greedyPathParam);
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
        if (getNonGreedyPathParam() != null)
            sb.append("NonGreedyPathParam: ").append(getNonGreedyPathParam()).append(",");
        if (getGreedyPathParam() != null)
            sb.append("GreedyPathParam: ").append(getGreedyPathParam());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof OperationWithGreedyLabelRequest == false)
            return false;
        OperationWithGreedyLabelRequest other = (OperationWithGreedyLabelRequest) obj;
        if (other.getNonGreedyPathParam() == null ^ this.getNonGreedyPathParam() == null)
            return false;
        if (other.getNonGreedyPathParam() != null && other.getNonGreedyPathParam().equals(this.getNonGreedyPathParam()) == false)
            return false;
        if (other.getGreedyPathParam() == null ^ this.getGreedyPathParam() == null)
            return false;
        if (other.getGreedyPathParam() != null && other.getGreedyPathParam().equals(this.getGreedyPathParam()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getNonGreedyPathParam() == null) ? 0 : getNonGreedyPathParam().hashCode());
        hashCode = prime * hashCode + ((getGreedyPathParam() == null) ? 0 : getGreedyPathParam().hashCode());
        return hashCode;
    }

    @Override
    public OperationWithGreedyLabelRequest clone() {
        return (OperationWithGreedyLabelRequest) super.clone();
    }

}
