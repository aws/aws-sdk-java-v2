/*
 * Copyright 2013 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.services.dynamodbv2.mapper;

import java.math.BigDecimal;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;

/**
 * Comprehensive domain class
 */
@DynamoDBTable(tableName = "aws-java-sdk-index-range-test")
public class IndexRangeKeyClass {

    private long key;
    private double rangeKey;
    private Double indexFooRangeKey;
    private Double indexBarRangeKey;
    private Double multipleIndexRangeKey;
    private Long version;

    private String fooAttribute;
    private String barAttribute;

    @DynamoDBHashKey
    public long getKey() {
        return key;
    }

    public void setKey(long key) {
        this.key = key;
    }

    @DynamoDBRangeKey
    public double getRangeKey() {
        return rangeKey;
    }

    public void setRangeKey(double rangeKey) {
        this.rangeKey = rangeKey;
    }
    
    @DynamoDBIndexRangeKey (
    		localSecondaryIndexName = "index_foo",
    		attributeName = "indexFooRangeKey"
    )
    public Double getIndexFooRangeKeyWithFakeName() {
        return indexFooRangeKey;
    }

    public void setIndexFooRangeKeyWithFakeName(Double indexFooRangeKey) {
        this.indexFooRangeKey = indexFooRangeKey;
    }
    
    @DynamoDBIndexRangeKey (
    		localSecondaryIndexName = "index_bar"
    )
    public Double getIndexBarRangeKey() {
        return indexBarRangeKey;
    }

    public void setIndexBarRangeKey(Double indexBarRangeKey) {
        this.indexBarRangeKey = indexBarRangeKey;
    }
    
    @DynamoDBIndexRangeKey (
    		localSecondaryIndexNames = {"index_foo_copy", "index_bar_copy"}
    )
    public Double getMultipleIndexRangeKey() {
        return multipleIndexRangeKey;
    }

    public void setMultipleIndexRangeKey(Double multipleIndexRangeKey) {
        this.multipleIndexRangeKey = multipleIndexRangeKey;
    }

    @DynamoDBAttribute
    public String getFooAttribute() {
        return fooAttribute;
    }

    public void setFooAttribute(String fooAttribute) {
        this.fooAttribute = fooAttribute;
    }

    @DynamoDBAttribute
    public String getBarAttribute() {
        return barAttribute;
    }

    public void setBarAttribute(String barAttribute) {
        this.barAttribute = barAttribute;
    }

    @DynamoDBVersionAttribute
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fooAttribute == null) ? 0 : fooAttribute.hashCode());
        result = prime * result + ((barAttribute == null) ? 0 : barAttribute.hashCode());
        result = prime * result + (int) (key ^ (key >>> 32));
        long temp;
        temp = Double.doubleToLongBits(rangeKey);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(indexFooRangeKey);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(indexBarRangeKey);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        IndexRangeKeyClass other = (IndexRangeKeyClass) obj;
        if ( fooAttribute == null ) {
            if ( other.fooAttribute != null )
                return false;
        } else if ( !fooAttribute.equals(other.fooAttribute) )
            return false;
        if ( barAttribute == null ) {
            if ( other.barAttribute != null )
                return false;
        } else if ( !barAttribute.equals(other.barAttribute) )
            return false;
        if ( key != other.key )
            return false;
        if ( Double.doubleToLongBits(rangeKey) != Double.doubleToLongBits(other.rangeKey) )
            return false;
        if ( Double.doubleToLongBits(indexFooRangeKey) != Double.doubleToLongBits(other.indexFooRangeKey) )
            return false;
        if ( Double.doubleToLongBits(indexBarRangeKey) != Double.doubleToLongBits(other.indexBarRangeKey) )
            return false;
        if ( version == null ) {
            if ( other.version != null )
                return false;
        } else if ( !version.equals(other.version) )
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "IndexRangeKeyClass [key=" + key + ", rangeKey=" + rangeKey + ", version=" + version
                + ", indexFooRangeKey=" + indexFooRangeKey + ", indexBarRangeKey=" + indexBarRangeKey
                + ", fooAttribute=" + fooAttribute + ", barAttribute=" + barAttribute + "]";
    }

}
