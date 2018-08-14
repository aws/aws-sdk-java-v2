/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.sfn.builder;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder.eq;
import static software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder.gt;
import static software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder.gte;
import static software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder.lt;
import static software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder.lte;

import org.junit.Assert;
import org.junit.Test;

public class NumericConditionsTest {

    @Test
    public void getExpectedValue_numericEquals_ReturnsIntegralString() {
        Assert.assertEquals("42", StepFunctionBuilder.eq("$.var", 42).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericGreaterThan_ReturnsIntegralString() {
        assertEquals("42", StepFunctionBuilder.gt("$.var", 42).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericGreaterThanEquals_ReturnsIntegralString() {
        Assert.assertEquals("42", StepFunctionBuilder.gte("$.var", 42).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericLessThan_ReturnsIntegralString() {
        Assert.assertEquals("42", StepFunctionBuilder.lt("$.var", 42).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericLessThanEquals_ReturnsIntegralString() {
        Assert.assertEquals("42", StepFunctionBuilder.lte("$.var", 42).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericEquals_ReturnsDoubleString() {
        Assert.assertEquals("9000.1", StepFunctionBuilder.eq("$.var", 9000.1).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericGreaterThan_ReturnsDoubleString() {
        assertEquals("9000.1", StepFunctionBuilder.gt("$.var", 9000.1).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericGreaterThanEquals_ReturnsDoubleString() {
        Assert.assertEquals("9000.1", StepFunctionBuilder.gte("$.var", 9000.1).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericLessThan_ReturnsDoubleString() {
        Assert.assertEquals("9000.1", StepFunctionBuilder.lt("$.var", 9000.1).build().getExpectedValue());
    }

    @Test
    public void getExpectedValue_numericLessThanEquals_ReturnsDoubleString() {
        Assert.assertEquals("9000.1", StepFunctionBuilder.lte("$.var", 9000.1).build().getExpectedValue());
    }
}
