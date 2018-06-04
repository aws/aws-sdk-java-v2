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

package software.amazon.awssdk.codegen.docs;

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.codegen.internal.Constant.LF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Builder for a Javadoc string that orders sections consistently.
 */
public final class DocumentationBuilder {

    // TODO This prefix is not suitable for paginated operations. Either remove it for paginated operations
    // or change the statement to something generic
    private static final String ASYNC_THROWS_PREFIX = "The CompletableFuture returned by this method can be completed " +
                                                      "exceptionally with the following exceptions.";

    private String desc;
    private List<Pair<String, String>> params = new ArrayList<>();
    private String returns;
    private List<Pair<String, String>> asyncThrows = new ArrayList<>();
    private List<Pair<String, String>> syncThrows = new ArrayList<>();
    private List<Pair<String, List<String>>> tags = new ArrayList<>();
    private List<String> see = new ArrayList<>();

    /**
     * Description of javaodc comment. I.E. what you are reading right now.
     *
     * @param docs Description string
     * @return This builder for method chaining.
     */
    public DocumentationBuilder description(String docs) {
        this.desc = docs;
        return this;
    }

    /**
     * Adds a new param to the Javadoc.
     *
     * @param paramName Name of parameter.
     * @param paramDocs Documentation for parameter.
     * @return This builder for method chaining.
     */
    public DocumentationBuilder param(String paramName, String paramDocs) {
        this.params.add(Pair.of(paramName, paramDocs));
        return this;
    }

    /**
     * Adds a new param to the Javadoc. Uses {@link String#format(String, Object...)} using the given arguments.
     *
     * @param paramName  Name of parameter.
     * @param paramDocs  Documentation for parameter.
     * @param formatArgs Arguments referenced by format specifiers.
     * @return This builder for method chaining.
     */
    public DocumentationBuilder param(String paramName, String paramDocs, Object... formatArgs) {
        return param(paramName, String.format(paramDocs, formatArgs));
    }

    /**
     * Adds documentation for return value. If not set then no return tag will be added to the Javadoc string.
     *
     * @param returnsDoc Documentation for return value (if present).
     * @return This builder for method chaining.
     */
    public DocumentationBuilder returns(String returnsDoc) {
        this.returns = returnsDoc;
        return this;
    }

    /**
     * Adds documentation for return value. If not set then no return tag will be added to the Javadoc string. Uses
     * {@link String#format(String, Object...)} using the given arguments.
     *
     * @param returnsDoc Documentation for return value (if present).
     * @param formatArgs Arguments referenced by format specifiers.
     * @return This builder for method chaining.
     */
    public DocumentationBuilder returns(String returnsDoc, Object... formatArgs) {
        return returns(String.format(returnsDoc, formatArgs));
    }

    /**
     * Async exceptions are not thrown from the method, rather the returned {@link java.util.concurrent.CompletableFuture} is
     * completed exceptionally ({@link java.util.concurrent.CompletableFuture#completeExceptionally(Throwable)}. Because of this
     * we don't add @throws to the Javadocs or method signature for async methods, we instead add a list of exceptions the future
     * may be completed exceptionally with in the @returns section of the Javadoc.
     *
     * @param exceptionClass Class name of thrown exception.
     * @param exceptionDoc   Documentation for thrown exception.
     * @return This builder for method chaining.
     */
    public DocumentationBuilder asyncThrows(String exceptionClass, String exceptionDoc) {
        return asyncThrows(singletonList(Pair.of(exceptionClass, exceptionDoc)));
    }

    /**
     * Adds multiple async throws to the Javadoc for each exception name / exception doc pair.
     *
     * @param exceptions Multiple pairs of exception name to exception documentation.
     * @return This builder for method chaining.
     * @see #asyncThrows(String, String)
     */
    public DocumentationBuilder asyncThrows(Pair<String, String>... exceptions) {
        return asyncThrows(Arrays.asList(exceptions));
    }

    /**
     * Adds multiple async throws to the Javadoc for each exception name / exception doc pair.
     *
     * @param exceptions Multiple pairs of exception name to exception documentation.
     * @return This builder for method chaining.
     * @see #asyncThrows(String, String)
     */
    public DocumentationBuilder asyncThrows(List<Pair<String, String>> exceptions) {
        asyncThrows.addAll(exceptions);
        return this;
    }

    /**
     * Adds a throws tag to the Javadoc.
     *
     * @param exceptionClass Class name of thrown exception.
     * @param exceptionDoc   Documentation for thrown exception.
     * @return This builder for method chaining.
     */
    public DocumentationBuilder syncThrows(String exceptionClass, String exceptionDoc) {
        return syncThrows(singletonList(Pair.of(exceptionClass, exceptionDoc)));
    }

    /**
     * Adds multiple throws tag to the Javadoc for each exception name / exception doc pair.
     *
     * @param exceptions Multiple pairs of exception name to exception documentation.
     * @return This builder for method chaining.
     * @see #syncThrows(String, String)
     */
    public DocumentationBuilder syncThrows(Pair<String, String>... exceptions) {
        return syncThrows(Arrays.asList(exceptions));
    }

    /**
     * Adds multiple throws tag to the Javadoc for each exception name / exception doc pair.
     *
     * @param exceptions Multiple pairs of exception name to exception documentation.
     * @return This builder for method chaining.
     * @see #syncThrows(String, String)
     */
    public DocumentationBuilder syncThrows(List<Pair<String, String>> exceptions) {
        syncThrows.addAll(exceptions);
        return this;
    }

    /**
     * Adds an arbitrary tag with values to the Javadoc. This will be added in between the throws and see sections
     * of the Javadoc.
     *
     * @param tagName   Name of tag to add.
     * @param tagValues List of values associated with the same.
     * @return This builder for method chaining.
     */
    public DocumentationBuilder tag(String tagName, String... tagValues) {
        tags.add(Pair.of(tagName, Arrays.asList(tagValues)));
        return this;
    }

    /**
     * Adds a @see reference to the Javadocs.
     *
     * @param seeLink Reference for @see.
     * @return This builder for method chaining.
     */
    public DocumentationBuilder see(String seeLink) {
        this.see.add(seeLink);
        return this;
    }

    /**
     * Adds a @see reference to the Javadocs. Uses {@link String#format(String, Object...)} using the given arguments.
     *
     * @param seeLink    Reference for @see.
     * @param formatArgs Arguments referenced by format specifiers.
     * @return This builder for method chaining.
     */
    public DocumentationBuilder see(String seeLink, Object... formatArgs) {
        return see(String.format(seeLink, formatArgs));
    }

    /**
     * Builds the Javadoc string with the current configuraton.
     *
     * @return Formatted Javadoc string.
     */
    public String build() {
        StringBuilder str = new StringBuilder();
        if (StringUtils.isNotBlank(desc)) {
            str.append(desc).append(LF).append(LF);
        }
        params.forEach(p -> p.apply((paramName, paramDoc) -> formatParam(str, paramName, paramDoc)));
        if (hasReturn() || !asyncThrows.isEmpty()) {
            str.append("@return ");
            if (hasReturn()) {
                str.append(returns);
            }
            if (!asyncThrows.isEmpty()) {
                // If no docs were provided for success returns add a generic one.
                if (!hasReturn()) {
                    str.append("A CompletableFuture indicating when result will be completed.");
                }
                appendAsyncThrows(str);
            }
            str.append(LF);
        }
        syncThrows.forEach(t -> t.apply((exName, exDoc) -> formatThrows(str, exName, exDoc)));
        tags.forEach(t -> t.apply((tagName, tagVals) -> formatTag(str, tagName, tagVals)));
        see.forEach(s -> str.append("@see ").append(s).append(LF));

        return str.toString();
    }

    private boolean hasReturn() {
        return StringUtils.isNotBlank(returns);
    }

    private void appendAsyncThrows(StringBuilder str) {
        str.append("<br/>").append(LF).append(
                asyncThrows.stream()
                           .map(t -> t.apply((exName, exDocs) -> String.format("<li>%s %s</li>", exName, exDocs)))
                           .collect(Collectors.joining(LF, ASYNC_THROWS_PREFIX + LF + "<ul>" + LF, LF + "</ul>")));
    }

    private StringBuilder formatParam(StringBuilder doc, String paramName, String paramDoc) {
        return doc.append("@param ").append(paramName).append(" ").append(paramDoc).append(LF);
    }

    private StringBuilder formatThrows(StringBuilder str, String exName, String exDoc) {
        return str.append("@throws ").append(exName).append(" ").append(exDoc).append(LF);
    }

    private StringBuilder formatTag(StringBuilder str, String tagName, List<String> tagVals) {
        return str.append("@").append(tagName).append(" ")
                  .append(tagVals.stream().collect(Collectors.joining(" ")))
                  .append(LF);
    }

}
