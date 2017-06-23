/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.model.intermediate;

import static software.amazon.awssdk.utils.StringUtils.upperCase;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.apache.commons.lang3.StringEscapeUtils;
import software.amazon.awssdk.codegen.JmesPathCodeGenVisitor;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.jmespath.JmesPathExpression;

public class AcceptorModel {

    private String matcher;

    private String argument;

    private String state;

    private JsonNode expected;

    private JmesPathExpression ast;

    public String getAst() {
        if (ast != null) {
            return ast.accept(new JmesPathCodeGenVisitor(), null);
        }
        return null;
    }

    public void setAst(JmesPathExpression ast) {
        this.ast = ast;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public JsonNode getExpected() {
        return expected;
    }

    public void setExpected(JsonNode expected) {
        this.expected = expected;
    }

    public String getExpectedAsCamelCase() {
        return Utils.capitialize(this.expected.asText().replaceAll("\\W", ""));
    }

    public String getExpectedAsEscapedJson() {
        return StringEscapeUtils.escapeJava(expected.toString());
    }

    public String getExpectedAsString() {
        if (expected.isTextual()) {
            return expected.toString();
        }
        return null;
    }

    public BigDecimal getExpectedAsNumber() {
        if (expected.isNumber()) {
            return expected.decimalValue();
        }
        return null;
    }

    public String getEnumState() {
        return "WaiterState." + upperCase(this.state);
    }

    public boolean getIsStatusMatcher() {
        return this.matcher.equals("status");
    }

    public boolean getIsErrorMatcher() {
        return this.matcher.equals("error");
    }
}
