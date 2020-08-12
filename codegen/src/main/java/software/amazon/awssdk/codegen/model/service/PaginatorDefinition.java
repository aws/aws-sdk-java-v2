/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.model.service;

import static java.util.Collections.singletonList;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.jr.stree.JrsArray;
import com.fasterxml.jackson.jr.stree.JrsValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents the structure for each operation in paginators-1.json file
 *
 * This class is used to generate auto-paginated APIs.
 */
public class PaginatorDefinition {

    private static final String VALID_REGEX = "[a-zA-Z.]+";
    /**
     * The members in the request which needs to be set to get the next page.
     */
    private List<String> inputToken;

    /**
     * The members in the response which are used to get the next page.
     */
    private List<String> outputToken;

    /**
     * The paginated list of members in the response
     */
    private List<String> resultKey;

    /**
     * The name of the member in the response that indicates if the response is truncated.
     * If the value of member is true, there are more results that can be retrieved.
     * If the value of member is false, then there are no additional results.
     *
     * This is an optional field. If this value is missing, use the outputToken instead to check
     * if more results are available or not.
     */
    private String moreResults;

    /**
     * The member in the request that is used to limit the number of results per page.
     */
    private String limitKey;

    public PaginatorDefinition() {
    }

    public List<String> getInputToken() {
        return inputToken;
    }

    public void setInputToken(List<String> inputToken) {
        this.inputToken = inputToken;
    }

    public List<String> getOutputToken() {
        return outputToken;
    }

    public void setOutputToken(List<String> outputToken) {
        this.outputToken = outputToken;
    }

    public List<String> getResultKey() {
        return resultKey;
    }

    public void setResultKey(List<String> resultKey) {
        this.resultKey = resultKey;
    }

    public String getMoreResults() {
        return moreResults;
    }

    public void setMoreResults(String moreResults) {
        this.moreResults = moreResults;
    }

    public String getLimitKey() {
        return limitKey;
    }

    public void setLimitKey(String limitKey) {
        this.limitKey = limitKey;
    }

    /**
     * Returns a boolean value indicating if the information present in this object
     * is sufficient to generate the paginated APIs.
     *
     * @return True if all necessary information to generate paginator APIs is present. Otherwise false.
     */
    public boolean isValid() {
        Pattern p = Pattern.compile(VALID_REGEX);

        return !CollectionUtils.isNullOrEmpty(inputToken) &&
               !CollectionUtils.isNullOrEmpty(outputToken) &&
               outputToken.stream().allMatch(t -> p.matcher(t).matches());
    }

    private List<String> asList(JrsValue node) {
        if (node.isArray()) {
            List<String> output = new ArrayList<>();
            Iterator<JrsValue> elements = ((JrsArray) node).elements();
            elements.forEachRemaining(v  -> output.add(asString(v)));
            return output;
        } else {
            return singletonList(asString(node));
        }
    }

    private String asString(JrsValue value) {
        Validate.isTrue(value.asToken() == JsonToken.VALUE_STRING, "Expected a string node: " + value);
        return value.asText();
    }

    // CHECKSTYLE:OFF - These are all gross versions of the setter methods that match the C2J name.
    /**
     * Gross version of {@link #setLimitKey} that matches the JSON attribute name.
     */
    public void setLimit_key(String limitKey) {
        this.limitKey = limitKey;
    }

    /**
     * Gross version of {@link #setInputToken} that matches the JSON attribute name.
     */
    public void setInput_token(JrsValue inputToken) {
        this.inputToken = asList(inputToken);
    }

    /**
     * Gross version of {@link #setOutputToken} that matches the JSON attribute name.
     */
    public void setOutput_token(JrsValue outputToken) {
        this.outputToken = asList(outputToken);
    }

    /**
     * Gross version of {@link #setResultKey} that matches the JSON attribute name.
     */
    public void setResult_key(JrsValue resultKey) {
        this.resultKey = asList(resultKey);
    }

    /**
     * Gross version of {@link #setMoreResults} that matches the JSON attribute name.
     */
    public void setMore_results(String moreResults) {
        this.moreResults = moreResults;
    }
    // CHECKSTYLE:ON
}
