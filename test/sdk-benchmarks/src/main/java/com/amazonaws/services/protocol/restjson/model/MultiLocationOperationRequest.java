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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MultiLocationOperation" target="_top">AWS
 *      API Documentation</a>
 */

public class MultiLocationOperationRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private String pathParam;

    private String queryParamOne;

    private String queryParamTwo;

    private String stringHeaderMember;

    private java.util.Date timestampHeaderMember;

    private PayloadStructType payloadStructParam;

    /**
     * @param pathParam
     */

    public void setPathParam(String pathParam) {
        this.pathParam = pathParam;
    }

    /**
     * @return
     */

    public String getPathParam() {
        return this.pathParam;
    }

    /**
     * @param pathParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MultiLocationOperationRequest withPathParam(String pathParam) {
        setPathParam(pathParam);
        return this;
    }

    /**
     * @param queryParamOne
     */

    public void setQueryParamOne(String queryParamOne) {
        this.queryParamOne = queryParamOne;
    }

    /**
     * @return
     */

    public String getQueryParamOne() {
        return this.queryParamOne;
    }

    /**
     * @param queryParamOne
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MultiLocationOperationRequest withQueryParamOne(String queryParamOne) {
        setQueryParamOne(queryParamOne);
        return this;
    }

    /**
     * @param queryParamTwo
     */

    public void setQueryParamTwo(String queryParamTwo) {
        this.queryParamTwo = queryParamTwo;
    }

    /**
     * @return
     */

    public String getQueryParamTwo() {
        return this.queryParamTwo;
    }

    /**
     * @param queryParamTwo
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MultiLocationOperationRequest withQueryParamTwo(String queryParamTwo) {
        setQueryParamTwo(queryParamTwo);
        return this;
    }

    /**
     * @param stringHeaderMember
     */

    public void setStringHeaderMember(String stringHeaderMember) {
        this.stringHeaderMember = stringHeaderMember;
    }

    /**
     * @return
     */

    public String getStringHeaderMember() {
        return this.stringHeaderMember;
    }

    /**
     * @param stringHeaderMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MultiLocationOperationRequest withStringHeaderMember(String stringHeaderMember) {
        setStringHeaderMember(stringHeaderMember);
        return this;
    }

    /**
     * @param timestampHeaderMember
     */

    public void setTimestampHeaderMember(java.util.Date timestampHeaderMember) {
        this.timestampHeaderMember = timestampHeaderMember;
    }

    /**
     * @return
     */

    public java.util.Date getTimestampHeaderMember() {
        return this.timestampHeaderMember;
    }

    /**
     * @param timestampHeaderMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MultiLocationOperationRequest withTimestampHeaderMember(java.util.Date timestampHeaderMember) {
        setTimestampHeaderMember(timestampHeaderMember);
        return this;
    }

    /**
     * @param payloadStructParam
     */

    public void setPayloadStructParam(PayloadStructType payloadStructParam) {
        this.payloadStructParam = payloadStructParam;
    }

    /**
     * @return
     */

    public PayloadStructType getPayloadStructParam() {
        return this.payloadStructParam;
    }

    /**
     * @param payloadStructParam
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MultiLocationOperationRequest withPayloadStructParam(PayloadStructType payloadStructParam) {
        setPayloadStructParam(payloadStructParam);
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
        if (getPathParam() != null)
            sb.append("PathParam: ").append(getPathParam()).append(",");
        if (getQueryParamOne() != null)
            sb.append("QueryParamOne: ").append(getQueryParamOne()).append(",");
        if (getQueryParamTwo() != null)
            sb.append("QueryParamTwo: ").append(getQueryParamTwo()).append(",");
        if (getStringHeaderMember() != null)
            sb.append("StringHeaderMember: ").append(getStringHeaderMember()).append(",");
        if (getTimestampHeaderMember() != null)
            sb.append("TimestampHeaderMember: ").append(getTimestampHeaderMember()).append(",");
        if (getPayloadStructParam() != null)
            sb.append("PayloadStructParam: ").append(getPayloadStructParam());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof MultiLocationOperationRequest == false)
            return false;
        MultiLocationOperationRequest other = (MultiLocationOperationRequest) obj;
        if (other.getPathParam() == null ^ this.getPathParam() == null)
            return false;
        if (other.getPathParam() != null && other.getPathParam().equals(this.getPathParam()) == false)
            return false;
        if (other.getQueryParamOne() == null ^ this.getQueryParamOne() == null)
            return false;
        if (other.getQueryParamOne() != null && other.getQueryParamOne().equals(this.getQueryParamOne()) == false)
            return false;
        if (other.getQueryParamTwo() == null ^ this.getQueryParamTwo() == null)
            return false;
        if (other.getQueryParamTwo() != null && other.getQueryParamTwo().equals(this.getQueryParamTwo()) == false)
            return false;
        if (other.getStringHeaderMember() == null ^ this.getStringHeaderMember() == null)
            return false;
        if (other.getStringHeaderMember() != null && other.getStringHeaderMember().equals(this.getStringHeaderMember()) == false)
            return false;
        if (other.getTimestampHeaderMember() == null ^ this.getTimestampHeaderMember() == null)
            return false;
        if (other.getTimestampHeaderMember() != null && other.getTimestampHeaderMember().equals(this.getTimestampHeaderMember()) == false)
            return false;
        if (other.getPayloadStructParam() == null ^ this.getPayloadStructParam() == null)
            return false;
        if (other.getPayloadStructParam() != null && other.getPayloadStructParam().equals(this.getPayloadStructParam()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getPathParam() == null) ? 0 : getPathParam().hashCode());
        hashCode = prime * hashCode + ((getQueryParamOne() == null) ? 0 : getQueryParamOne().hashCode());
        hashCode = prime * hashCode + ((getQueryParamTwo() == null) ? 0 : getQueryParamTwo().hashCode());
        hashCode = prime * hashCode + ((getStringHeaderMember() == null) ? 0 : getStringHeaderMember().hashCode());
        hashCode = prime * hashCode + ((getTimestampHeaderMember() == null) ? 0 : getTimestampHeaderMember().hashCode());
        hashCode = prime * hashCode + ((getPayloadStructParam() == null) ? 0 : getPayloadStructParam().hashCode());
        return hashCode;
    }

    @Override
    public MultiLocationOperationRequest clone() {
        return (MultiLocationOperationRequest) super.clone();
    }

}
