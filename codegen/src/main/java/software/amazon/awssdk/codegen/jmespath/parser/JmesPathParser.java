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

package software.amazon.awssdk.codegen.jmespath.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.jmespath.component.AndExpression;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifier;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithQuestionMark;
import software.amazon.awssdk.codegen.jmespath.component.Comparator;
import software.amazon.awssdk.codegen.jmespath.component.ComparatorExpression;
import software.amazon.awssdk.codegen.jmespath.component.CurrentNode;
import software.amazon.awssdk.codegen.jmespath.component.Expression;
import software.amazon.awssdk.codegen.jmespath.component.ExpressionType;
import software.amazon.awssdk.codegen.jmespath.component.FunctionArg;
import software.amazon.awssdk.codegen.jmespath.component.FunctionExpression;
import software.amazon.awssdk.codegen.jmespath.component.IndexExpression;
import software.amazon.awssdk.codegen.jmespath.component.KeyValueExpression;
import software.amazon.awssdk.codegen.jmespath.component.Literal;
import software.amazon.awssdk.codegen.jmespath.component.MultiSelectHash;
import software.amazon.awssdk.codegen.jmespath.component.MultiSelectList;
import software.amazon.awssdk.codegen.jmespath.component.NotExpression;
import software.amazon.awssdk.codegen.jmespath.component.OrExpression;
import software.amazon.awssdk.codegen.jmespath.component.ParenExpression;
import software.amazon.awssdk.codegen.jmespath.component.PipeExpression;
import software.amazon.awssdk.codegen.jmespath.component.SliceExpression;
import software.amazon.awssdk.codegen.jmespath.component.SubExpression;
import software.amazon.awssdk.codegen.jmespath.component.SubExpressionRight;
import software.amazon.awssdk.codegen.jmespath.component.WildcardExpression;
import software.amazon.awssdk.codegen.jmespath.parser.util.CompositeParser;
import software.amazon.awssdk.utils.Logger;

/**
 * Parses a JMESPath expression string into an {@link Expression}.
 *
 * This implements the grammar described here: https://jmespath.org/specification.html#grammar
 */
public class JmesPathParser {
    private static final Logger log = Logger.loggerFor(JmesPathParser.class);

    private final String input;

    private JmesPathParser(String input) {
        this.input = input;
    }

    /**
     * Parses a JMESPath expression string into a {@link Expression}.
     */
    public static Expression parse(String jmesPathString) {
        return new JmesPathParser(jmesPathString).parse();
    }

    private Expression parse() {
        ParseResult<Expression> expression = parseExpression(0, input.length());
        if (!expression.hasResult()) {
            throw new IllegalArgumentException("Failed to parse expression.");
        }

        return expression.result();
    }

    /**
     * expression        = sub-expression / index-expression  / comparator-expression
     * expression        =/ or-expression / identifier
     * expression        =/ and-expression / not-expression / paren-expression
     * expression        =/ "*" / multi-select-list / multi-select-hash / literal
     * expression        =/ function-expression / pipe-expression / raw-string
     * expression        =/ current-node
     */
    private ParseResult<Expression> parseExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (startPosition < 0 || endPosition > input.length() + 1) {
            return ParseResult.error();
        }

        return CompositeParser.firstTry(this::parseSubExpression, Expression::subExpression)
                              .thenTry(this::parseIndexExpression, Expression::indexExpression)
                              .thenTry(this::parseNotExpression, Expression::notExpression)
                              .thenTry(this::parseAndExpression, Expression::andExpression)
                              .thenTry(this::parseOrExpression, Expression::orExpression)
                              .thenTry(this::parseComparatorExpression, Expression::comparatorExpression)
                              .thenTry(this::parsePipeExpression, Expression::pipeExpression)
                              .thenTry(this::parseIdentifier, Expression::identifier)
                              .thenTry(this::parseParenExpression, Expression::parenExpression)
                              .thenTry(this::parseWildcardExpression, Expression::wildcardExpression)
                              .thenTry(this::parseMultiSelectList, Expression::multiSelectList)
                              .thenTry(this::parseMultiSelectHash, Expression::multiSelectHash)
                              .thenTry(this::parseLiteral, Expression::literal)
                              .thenTry(this::parseFunctionExpression, Expression::functionExpression)
                              .thenTry(this::parseRawString, Expression::rawString)
                              .thenTry(this::parseCurrentNode, Expression::currentNode)
                              .parse(startPosition, endPosition);
    }

    /**
     * sub-expression    = expression "." ( identifier /
     * multi-select-list /
     * multi-select-hash /
     * function-expression /
     * "*" )
     */
    private ParseResult<SubExpression> parseSubExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        List<Integer> dotPositions = findCharacters(startPosition + 1, endPosition - 1, ".");
        for (Integer dotPosition : dotPositions) {
            ParseResult<Expression> leftSide = parseExpression(startPosition, dotPosition);
            if (!leftSide.hasResult()) {
                continue;
            }

            ParseResult<SubExpressionRight> rightSide =
                CompositeParser.firstTry(this::parseIdentifier, SubExpressionRight::identifier)
                               .thenTry(this::parseMultiSelectList, SubExpressionRight::multiSelectList)
                               .thenTry(this::parseMultiSelectHash, SubExpressionRight::multiSelectHash)
                               .thenTry(this::parseFunctionExpression, SubExpressionRight::functionExpression)
                               .thenTry(this::parseWildcardExpression, SubExpressionRight::wildcardExpression)
                               .parse(dotPosition + 1, endPosition);

            if (!rightSide.hasResult()) {
                continue;
            }

            return ParseResult.success(new SubExpression(leftSide.result(), rightSide.result()));
        }

        logError("sub-expression", "Invalid sub-expression", startPosition);
        return ParseResult.error();
    }

    /**
     * pipe-expression   = expression "|" expression
     */
    private ParseResult<PipeExpression> parsePipeExpression(int startPosition, int endPosition) {
        return parseBinaryExpression(startPosition, endPosition, "|", PipeExpression::new);
    }

    /**
     * or-expression     = expression "||" expression
     */
    private ParseResult<OrExpression> parseOrExpression(int startPosition, int endPosition) {
        return parseBinaryExpression(startPosition, endPosition, "||", OrExpression::new);
    }

    /**
     * and-expression    = expression "&&" expression
     */
    private ParseResult<AndExpression> parseAndExpression(int startPosition, int endPosition) {
        return parseBinaryExpression(startPosition, endPosition, "&&", AndExpression::new);
    }

    private <T> ParseResult<T> parseBinaryExpression(int startPosition, int endPosition, String delimiter,
                                                     BiFunction<Expression, Expression, T> constructor) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        List<Integer> delimiterPositions = findCharacters(startPosition + 1, endPosition - 1, delimiter);
        for (Integer delimiterPosition : delimiterPositions) {
            ParseResult<Expression> leftSide = parseExpression(startPosition, delimiterPosition);
            if (!leftSide.hasResult()) {
                continue;
            }

            ParseResult<Expression> rightSide = parseExpression(delimiterPosition + delimiter.length(), endPosition);
            if (!rightSide.hasResult()) {
                continue;
            }

            return ParseResult.success(constructor.apply(leftSide.result(), rightSide.result()));
        }

        logError("binary-expression", "Invalid binary-expression", startPosition);
        return ParseResult.error();
    }

    /**
     * not-expression    = "!" expression
     */
    private ParseResult<NotExpression> parseNotExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (!startsWith(startPosition, '!')) {
            logError("not-expression", "Expected '!'", startPosition);
            return ParseResult.error();
        }

        return parseExpression(startPosition + 1, endPosition).mapResult(NotExpression::new);
    }

    /**
     * paren-expression  = "(" expression ")"
     */
    private ParseResult<ParenExpression> parseParenExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (!startsAndEndsWith(startPosition, endPosition, '(', ')')) {
            logError("paren-expression", "Expected '(' and ')'", startPosition);
            return ParseResult.error();
        }

        return parseExpression(startPosition + 1, endPosition - 1).mapResult(ParenExpression::new);
    }

    /**
     * index-expression  = expression bracket-specifier / bracket-specifier
     */
    private ParseResult<IndexExpression> parseIndexExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        return CompositeParser.firstTry(this::parseIndexExpressionWithLhsExpression)
                              .thenTry(this::parseBracketSpecifier, b -> IndexExpression.indexExpression(null, b))
                              .parse(startPosition, endPosition);
    }

    /**
     * expression bracket-specifier
     */
    private ParseResult<IndexExpression> parseIndexExpressionWithLhsExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        List<Integer> bracketPositions = findCharacters(startPosition + 1, endPosition - 1, "[");
        for (Integer bracketPosition : bracketPositions) {
            ParseResult<Expression> leftSide = parseExpression(startPosition, bracketPosition);
            if (!leftSide.hasResult()) {
                continue;
            }

            ParseResult<BracketSpecifier> rightSide = parseBracketSpecifier(bracketPosition, endPosition);
            if (!rightSide.hasResult()) {
                continue;
            }

            return ParseResult.success(IndexExpression.indexExpression(leftSide.result(), rightSide.result()));
        }

        logError("index-expression with lhs-expression", "Invalid index-expression with lhs-expression", startPosition);
        return ParseResult.error();
    }

    /**
     * multi-select-list = "[" ( expression *( "," expression ) ) "]"
     */
    private ParseResult<MultiSelectList> parseMultiSelectList(int startPosition, int endPosition) {
        return parseMultiSelect(startPosition, endPosition, '[', ']', this::parseExpression)
            .mapResult(MultiSelectList::new);
    }

    /**
     * multi-select-hash = "{" ( keyval-expr *( "," keyval-expr ) ) "}"
     */
    private ParseResult<MultiSelectHash> parseMultiSelectHash(int startPosition, int endPosition) {
        return parseMultiSelect(startPosition, endPosition, '{', '}', this::parseKeyValueExpression)
            .mapResult(MultiSelectHash::new);
    }

    /**
     * Parses "startDelimiter" ( entryParserType *( "," entryParserType ) ) "endDelimiter"
     * <p>
     * Used by {@link #parseMultiSelectHash}, {@link #parseMultiSelectList}.
     */
    private <T> ParseResult<List<T>> parseMultiSelect(int startPosition, int endPosition,
                                                      char startDelimiter, char endDelimiter,
                                                      Parser<T> entryParser) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (!startsAndEndsWith(startPosition, endPosition, startDelimiter, endDelimiter)) {
            logError("multi-select", "Expected '" + startDelimiter + "' and '" + endDelimiter + "'", startPosition);
            return ParseResult.error();
        }

        List<Integer> commaPositions = findCharacters(startPosition + 1, endPosition - 1, ",");

        if (commaPositions.isEmpty()) {
            return entryParser.parse(startPosition + 1, endPosition - 1).mapResult(Collections::singletonList);
        }

        List<T> results = new ArrayList<>();

        // Find first valid entries before a comma
        int startOfSecondEntry = -1;
        for (Integer comma : commaPositions) {
            ParseResult<T> result = entryParser.parse(startPosition + 1, comma);
            if (!result.hasResult()) {
                continue;
            }

            results.add(result.result());
            startOfSecondEntry = comma + 1;
        }

        if (results.size() == 0) {
            logError("multi-select", "Invalid value", startPosition + 1);
            return ParseResult.error();
        }

        if (results.size() > 1) {
            logError("multi-select", "Ambiguous separation", startPosition);
            return ParseResult.error();
        }

        // Find any subsequent entries
        int startPositionAfterComma = startOfSecondEntry;
        for (Integer commaPosition : commaPositions) {
            if (startPositionAfterComma > commaPosition) {
                continue;
            }

            ParseResult<T> entry = entryParser.parse(startPositionAfterComma, commaPosition);
            if (!entry.hasResult()) {
                continue;
            }

            results.add(entry.result());

            startPositionAfterComma = commaPosition + 1;
        }

        ParseResult<T> entry = entryParser.parse(startPositionAfterComma, endPosition - 1);
        if (!entry.hasResult()) {
            logError("multi-select", "Ambiguous separation", startPosition);
            return ParseResult.error();
        }
        results.add(entry.result());

        return ParseResult.success(results);
    }

    /**
     * keyval-expr       = identifier ":" expression
     */
    private ParseResult<KeyValueExpression> parseKeyValueExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        List<Integer> delimiterPositions = findCharacters(startPosition + 1, endPosition - 1, ":");
        for (Integer delimiterPosition : delimiterPositions) {
            ParseResult<String> identifier = parseIdentifier(startPosition, delimiterPosition);
            if (!identifier.hasResult()) {
                continue;
            }

            ParseResult<Expression> expression = parseExpression(delimiterPosition + 1, endPosition);
            if (!expression.hasResult()) {
                continue;
            }

            return ParseResult.success(new KeyValueExpression(identifier.result(), expression.result()));
        }

        logError("keyval-expr", "Invalid keyval-expr", startPosition);
        return ParseResult.error();
    }

    /**
     * bracket-specifier = "[" (number / "*" / slice-expression) "]" / "[]"
     * bracket-specifier =/ "[?" expression "]"
     */
    private ParseResult<BracketSpecifier> parseBracketSpecifier(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (!startsAndEndsWith(startPosition, endPosition, '[', ']')) {
            logError("bracket-specifier", "Expecting '[' and ']'", startPosition);
            return ParseResult.error();
        }

        // "[]"
        if (charsInRange(startPosition, endPosition) == 2) {
            return ParseResult.success(BracketSpecifier.withoutContents());
        }

        // "[?" expression "]"
        if (input.charAt(startPosition + 1) == '?') {
            return parseExpression(startPosition + 2, endPosition - 1)
                .mapResult(e -> BracketSpecifier.withQuestionMark(new BracketSpecifierWithQuestionMark(e)));
        }

        // "[" (number / "*" / slice-expression) "]"
        return CompositeParser.firstTry(this::parseNumber, BracketSpecifier::withNumberContents)
                              .thenTry(this::parseWildcardExpression, BracketSpecifier::withWildcardExpressionContents)
                              .thenTry(this::parseSliceExpression, BracketSpecifier::withSliceExpressionContents)
                              .parse(startPosition + 1, endPosition - 1);
    }

    /**
     * comparator-expression = expression comparator expression
     */
    private ParseResult<ComparatorExpression> parseComparatorExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        for (Comparator comparator : Comparator.values()) {
            List<Integer> comparatorPositions = findCharacters(startPosition, endPosition, comparator.tokenSymbol());

            for (Integer comparatorPosition : comparatorPositions) {
                ParseResult<Expression> lhsExpression = parseExpression(startPosition, comparatorPosition);
                if (!lhsExpression.hasResult()) {
                    continue;
                }

                ParseResult<Expression> rhsExpression =
                    parseExpression(comparatorPosition + comparator.tokenSymbol().length(), endPosition);
                if (!rhsExpression.hasResult()) {
                    continue;
                }

                return ParseResult.success(new ComparatorExpression(lhsExpression.result(),
                                                                    comparator,
                                                                    rhsExpression.result()));
            }
        }


        logError("comparator-expression", "Invalid comparator expression", startPosition);
        return ParseResult.error();
    }

    /**
     * slice-expression  = [number] ":" [number] [ ":" [number] ]
     */
    private ParseResult<SliceExpression> parseSliceExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        // Find the first colon
        int firstColonIndex = input.indexOf(':', startPosition);
        if (firstColonIndex < 0 || firstColonIndex >= endPosition) {
            logError("slice-expression", "Expected slice expression", startPosition);
            return ParseResult.error();
        }

        // Find the second colon (if it exists)
        int maybeSecondColonIndex = input.indexOf(':', firstColonIndex + 1);
        OptionalInt secondColonIndex = maybeSecondColonIndex < 0 || maybeSecondColonIndex >= endPosition
                                       ? OptionalInt.empty()
                                       : OptionalInt.of(maybeSecondColonIndex);

        // Find the first number bounds (if it exists)
        int firstNumberStart = startPosition;
        int firstNumberEnd = firstColonIndex;

        // Find the second number bounds (if it exists)
        int secondNumberStart = firstColonIndex + 1;
        int secondNumberEnd = secondColonIndex.orElse(endPosition);

        // Find the third number bounds (if it exists)
        int thirdNumberStart = secondColonIndex.orElse(endPosition) + 1;
        int thirdNumberEnd = endPosition;

        // Parse the first number (if it exists)
        Optional<Integer> firstNumber = Optional.empty();
        if (firstNumberStart < firstNumberEnd) {
            ParseResult<Integer> firstNumberParse = parseNumber(firstNumberStart, firstNumberEnd);
            if (!firstNumberParse.hasResult()) {
                return ParseResult.error();
            }
            firstNumber = Optional.of(firstNumberParse.result());
        }

        // Parse the second number (if it exists)
        Optional<Integer> secondNumber = Optional.empty();
        if (secondNumberStart < secondNumberEnd) {
            ParseResult<Integer> secondNumberParse = parseNumber(secondNumberStart, secondNumberEnd);
            if (!secondNumberParse.hasResult()) {
                return ParseResult.error();
            }
            secondNumber = Optional.of(secondNumberParse.result());
        }

        // Parse the third number (if it exists)
        Optional<Integer> thirdNumber = Optional.empty();
        if (thirdNumberStart < thirdNumberEnd) {
            ParseResult<Integer> thirdNumberParse = parseNumber(thirdNumberStart, thirdNumberEnd);
            if (!thirdNumberParse.hasResult()) {
                return ParseResult.error();
            }
            thirdNumber = Optional.of(thirdNumberParse.result());
        }

        return ParseResult.success(new SliceExpression(firstNumber.orElse(null),
                                                       secondNumber.orElse(null),
                                                       thirdNumber.orElse(null)));
    }

    /**
     * function-expression = unquoted-string ( no-args / one-or-more-args )
     */
    private ParseResult<FunctionExpression> parseFunctionExpression(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        int paramIndex = input.indexOf('(', startPosition);
        if (paramIndex <= 0) {
            logError("function-expression", "Expected function", startPosition);
            return ParseResult.error();
        }

        ParseResult<String> functionNameParse = parseUnquotedString(startPosition, paramIndex);
        if (!functionNameParse.hasResult()) {
            logError("function-expression", "Expected valid function name", startPosition);
            return ParseResult.error();
        }

        return CompositeParser.firstTry(this::parseNoArgs)
                              .thenTry(this::parseOneOrMoreArgs)
                              .parse(paramIndex, endPosition)
                              .mapResult(args -> new FunctionExpression(functionNameParse.result(), args));
    }

    /**
     * no-args             = "(" ")"
     */
    private ParseResult<List<FunctionArg>> parseNoArgs(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (!startsWith(startPosition, '(')) {
            logError("no-args", "Expected '('", startPosition);
            return ParseResult.error();
        }

        int closePosition = trimLeftWhitespace(startPosition + 1, endPosition);

        if (input.charAt(closePosition) != ')') {
            logError("no-args", "Expected ')'", closePosition);
            return ParseResult.error();
        }

        if (closePosition + 1 != endPosition) {
            logError("no-args", "Unexpected character", closePosition + 1);
            return ParseResult.error();
        }

        return ParseResult.success(Collections.emptyList());
    }

    /**
     * one-or-more-args    = "(" ( function-arg *( "," function-arg ) ) ")"
     */
    private ParseResult<List<FunctionArg>> parseOneOrMoreArgs(int startPosition, int endPosition) {
        return parseMultiSelect(startPosition, endPosition, '(', ')', this::parseFunctionArg);
    }

    /**
     * function-arg        = expression / expression-type
     */
    private ParseResult<FunctionArg> parseFunctionArg(int startPosition, int endPosition) {
        return CompositeParser.firstTry(this::parseExpression, FunctionArg::expression)
                              .thenTry(this::parseExpressionType, FunctionArg::expressionType)
                              .parse(startPosition, endPosition);
    }

    /**
     * current-node        = "@"
     */
    private ParseResult<CurrentNode> parseCurrentNode(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        return parseExpectedToken("current-node", startPosition, endPosition, '@').mapResult(x -> new CurrentNode());
    }

    /**
     * expression-type     = "&" expression
     */
    private ParseResult<ExpressionType> parseExpressionType(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (!startsWith(startPosition, '&')) {
            logError("expression-type", "Expected '&'", startPosition);
            return ParseResult.error();
        }

        return parseExpression(startPosition + 1, endPosition).mapResult(ExpressionType::new);
    }

    /**
     * raw-string        = "'" *raw-string-char "'"
     */
    private ParseResult<String> parseRawString(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (charsInRange(startPosition, endPosition) < 2) {
            logError("raw-string", "Invalid length", startPosition);
            return ParseResult.error();
        }

        if (!startsAndEndsWith(startPosition, endPosition, '\'', '\'')) {
            logError("raw-string", "Expected opening and closing \"'\"", startPosition);
            return ParseResult.error();
        }

        if (charsInRange(startPosition, endPosition) == 2) {
            return ParseResult.success("");
        }

        return parseRawStringChars(startPosition + 1, endPosition - 1);
    }

    /**
     * raw-string-char   = (%x20-26 / %x28-5B / %x5D-10FFFF) / preserved-escape / raw-string-escape
     */
    private ParseResult<String> parseRawStringChars(int startPosition, int endPosition) {
        StringBuilder result = new StringBuilder();
        for (int i = startPosition; i < endPosition; i++) {
            ParseResult<String> rawStringChar = parseLegalRawStringChar(i, i + 1);
            if (rawStringChar.hasResult()) {
                result.append(rawStringChar.result());
                continue;
            }

            ParseResult<String> preservedEscape = parsePreservedEscape(i, i + 2);
            if (preservedEscape.hasResult()) {
                result.append(preservedEscape.result());
                ++i;
                continue;
            }

            ParseResult<String> rawStringEscape = parseRawStringEscape(i, i + 2);
            if (rawStringEscape.hasResult()) {
                result.append(rawStringEscape.result());
                ++i;
                continue;
            }

            logError("raw-string", "Unexpected character", i);
            return ParseResult.error();
        }

        return ParseResult.success(result.toString());
    }

    /**
     * %x20-26 / %x28-5B / %x5D-10FFFF
     */
    private ParseResult<String> parseLegalRawStringChar(int startPosition, int endPosition) {
        if (charsInRange(startPosition, endPosition) != 1) {
            logError("raw-string-chars", "Invalid bounds", startPosition);
            return ParseResult.error();
        }

        if (!isLegalRawStringChar(input.charAt(startPosition))) {
            logError("raw-string-chars", "Invalid character in sequence", startPosition);
            return ParseResult.error();
        }

        return ParseResult.success(input.substring(startPosition, endPosition));
    }

    private boolean isLegalRawStringChar(char c) {
        return (c >= 0x20 && c <= 0x26) ||
               (c >= 0x28 && c <= 0x5B) ||
               (c >= 0x5D);
    }

    /**
     * preserved-escape  = escape (%x20-26 / %28-5B / %x5D-10FFFF)
     */
    private ParseResult<String> parsePreservedEscape(int startPosition, int endPosition) {
        if (endPosition > input.length()) {
            logError("preserved-escape", "Invalid end position", startPosition);
            return ParseResult.error();
        }

        if (charsInRange(startPosition, endPosition) != 2) {
            logError("preserved-escape", "Invalid length", startPosition);
            return ParseResult.error();
        }

        if (!startsWith(startPosition, '\\')) {
            logError("preserved-escape", "Expected \\", startPosition);
            return ParseResult.error();
        }

        return parseLegalRawStringChar(startPosition + 1, endPosition).mapResult(v -> "\\" + v);
    }

    /**
     * raw-string-escape = escape ("'" / escape)
     */
    private ParseResult<String> parseRawStringEscape(int startPosition, int endPosition) {
        if (endPosition > input.length()) {
            logError("preserved-escape", "Invalid end position", startPosition);
            return ParseResult.error();
        }

        if (charsInRange(startPosition, endPosition) != 2) {
            logError("raw-string-escape", "Invalid length", startPosition);
            return ParseResult.error();
        }

        if (!startsWith(startPosition, '\\')) {
            logError("raw-string-escape", "Expected '\\'", startPosition);
            return ParseResult.error();
        }

        if (input.charAt(startPosition + 1) != '\'' && input.charAt(startPosition + 1) != '\\') {
            logError("raw-string-escape", "Expected \"'\" or \"\\\"", startPosition);
            return ParseResult.error();
        }

        return ParseResult.success(input.substring(startPosition, endPosition));
    }

    /**
     * literal           = "`" json-value "`"
     */
    private ParseResult<Literal> parseLiteral(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (charsInRange(startPosition, endPosition) < 2) {
            logError("literal", "Invalid bounds", startPosition);
            return ParseResult.error();
        }

        if (!startsAndEndsWith(startPosition, endPosition, '`', '`')) {
            logError("literal", "Expected opening and closing '`'", startPosition);
            return ParseResult.error();
        }

        StringBuilder jsonString = new StringBuilder();
        for (int i = startPosition + 1; i < endPosition - 1; i++) {
            char character = input.charAt(i);
            if (character == '`') {
                int lastChar = i - 1;
                if (lastChar <= 0) {
                    logError("literal", "Unexpected '`'", startPosition);
                    return ParseResult.error();
                }

                int escapeCount = 0;
                for (int j = i - 1; j >= startPosition; j--) {
                    if (input.charAt(j) == '\\') {
                        ++escapeCount;
                    } else {
                        break;
                    }
                }

                if (escapeCount % 2 == 0) {
                    logError("literal", "Unescaped '`'", startPosition);
                    return ParseResult.error();
                }

                jsonString.setLength(jsonString.length() - 1); // Remove escape.
                jsonString.append('`');
            } else {
                jsonString.append(character);
            }
        }

        try {
            return ParseResult.success(new Literal(Jackson.readJrsValue(jsonString.toString())));
        } catch (IOException e) {
            logError("literal", "Invalid JSON: " + e.getMessage(), startPosition);
            return ParseResult.error();
        }
    }

    /**
     * number            = ["-"]1*digit
     * digit             = %x30-39
     */
    private ParseResult<Integer> parseNumber(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (startsWith(startPosition, '-')) {
            return parseNonNegativeNumber(startPosition + 1, endPosition).mapResult(i -> -i);
        }

        return parseNonNegativeNumber(startPosition, endPosition);
    }

    private ParseResult<Integer> parseNonNegativeNumber(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (charsInRange(startPosition, endPosition) < 1) {
            logError("number", "Expected number", startPosition);
            return ParseResult.error();
        }

        try {
            return ParseResult.success(Integer.parseInt(input.substring(startPosition, endPosition)));
        } catch (NumberFormatException e) {
            logError("number", "Expected number", startPosition);
            return ParseResult.error();
        }
    }

    /**
     * identifier        = unquoted-string / quoted-string
     */
    private ParseResult<String> parseIdentifier(int startPosition, int endPosition) {
        return CompositeParser.firstTry(this::parseUnquotedString)
                              .thenTry(this::parseQuotedString)
                              .parse(startPosition, endPosition);
    }

    /**
     * unquoted-string   = (%x41-5A / %x61-7A / %x5F) *(  ; A-Za-z_
     * %x30-39  /  ; 0-9
     * %x41-5A /  ; A-Z
     * %x5F    /  ; _
     * %x61-7A)   ; a-z
     */
    private ParseResult<String> parseUnquotedString(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (charsInRange(startPosition, endPosition) < 1) {
            logError("unquoted-string", "Invalid unquoted-string", startPosition);
            return ParseResult.error();
        }

        char firstToken = input.charAt(startPosition);
        if (!Character.isLetter(firstToken) && firstToken != '_') {
            logError("unquoted-string", "Unescaped strings must start with [A-Za-z_]", startPosition);
            return ParseResult.error();
        }

        for (int i = startPosition; i < endPosition; i++) {
            char c = input.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                logError("unquoted-string", "Invalid character in unescaped-string", i);
                return ParseResult.error();
            }
        }

        return ParseResult.success(input.substring(startPosition, endPosition));
    }

    /**
     * quoted-string     = quote 1*(unescaped-char / escaped-char) quote
     * quote = '"'
     */
    private ParseResult<String> parseQuotedString(int startPosition, int endPosition) {
        startPosition = trimLeftWhitespace(startPosition, endPosition);
        endPosition = trimRightWhitespace(startPosition, endPosition);

        if (!startsAndEndsWith(startPosition, endPosition, '"', '"')) {
            logError("quoted-string", "Expected opening and closing '\"'", startPosition);
            return ParseResult.error();
        }

        int stringStart = startPosition + 1;
        int stringEnd = endPosition - 1;

        int stringTokenCount = charsInRange(stringStart, stringEnd);
        if (stringTokenCount < 1) {
            logError("quoted-string", "Invalid quoted-string", startPosition);
            return ParseResult.error();
        }

        StringBuilder result = new StringBuilder();
        for (int i = stringStart; i < stringEnd; i++) {
            ParseResult<String> unescapedChar = parseUnescapedChar(i, i + 1);
            if (unescapedChar.hasResult()) {
                result.append(unescapedChar.result());
                continue;
            }

            ParseResult<String> escapedChar = parseEscapedChar(i, i + 2);
            if (escapedChar.hasResult()) {
                result.append(escapedChar.result());
                ++i;
                continue;
            }

            ParseResult<String> escapedUnicodeSequence = parseEscapedUnicodeSequence(i, i + 6);
            if (escapedUnicodeSequence.hasResult()) {
                result.append(escapedUnicodeSequence.result());
                i += 5;
                continue;
            }

            if (input.charAt(i) == '\\') {
                logError("quoted-string", "Unsupported escape sequence", i);
            } else {
                logError("quoted-string", "Unexpected character", i);
            }
            return ParseResult.error();
        }

        return ParseResult.success(result.toString());
    }

    /**
     * unescaped-char    = %x20-21 / %x23-5B / %x5D-10FFFF
     */
    private ParseResult<String> parseUnescapedChar(int startPosition, int endPosition) {
        for (int i = startPosition; i < endPosition; i++) {
            if (!isLegalUnescapedChar(input.charAt(i))) {
                logError("unescaped-char", "Invalid character in sequence", startPosition);
                return ParseResult.error();
            }
        }

        return ParseResult.success(input.substring(startPosition, endPosition));
    }

    private boolean isLegalUnescapedChar(char c) {
        return (c >= 0x20 && c <= 0x21) ||
               (c >= 0x23 && c <= 0x5B) ||
               (c >= 0x5D);
    }

    /**
     * escaped-char      = escape (
     * %x22 /          ; "    quotation mark  U+0022
     * %x5C /          ; \    reverse solidus U+005C
     * %x2F /          ; /    solidus         U+002F
     * %x62 /          ; b    backspace       U+0008
     * %x66 /          ; f    form feed       U+000C
     * %x6E /          ; n    line feed       U+000A
     * %x72 /          ; r    carriage return U+000D
     * %x74 /          ; t    tab             U+0009
     * %x75 4HEXDIG )  ; uXXXX                U+XXXX (this is handled as part of parseEscapedUnicodeSequence)
     */
    private ParseResult<String> parseEscapedChar(int startPosition, int endPosition) {
        if (endPosition > input.length()) {
            logError("escaped-char", "Invalid end position", startPosition);
            return ParseResult.error();
        }

        if (charsInRange(startPosition, endPosition) != 2) {
            logError("escaped-char", "Invalid length", startPosition);
            return ParseResult.error();
        }

        if (!startsWith(startPosition, '\\')) {
            logError("escaped-char", "Expected '\\'", startPosition);
            return ParseResult.error();
        }

        char escapedChar = input.charAt(startPosition + 1);
        switch (escapedChar) {
            case '"': return ParseResult.success("\"");
            case '\\': return ParseResult.success("\\");
            case '/': return ParseResult.success("/");
            case 'b': return ParseResult.success("\b");
            case 'f': return ParseResult.success("\f");
            case 'n': return ParseResult.success("\n");
            case 'r': return ParseResult.success("\r");
            case 't': return ParseResult.success("\t");
            default:
                logError("escaped-char", "Invalid escape sequence", startPosition);
                return ParseResult.error();
        }
    }

    private ParseResult<String> parseEscapedUnicodeSequence(int startPosition, int endPosition) {
        if (endPosition > input.length()) {
            logError("escaped-unicode-sequence", "Invalid end position", startPosition);
            return ParseResult.error();
        }

        if (charsInRange(startPosition, endPosition) != 6) {
            logError("escaped-unicode-sequence", "Invalid length", startPosition);
            return ParseResult.error();
        }

        if (input.charAt(startPosition) != '\\') {
            logError("escaped-unicode-sequence", "Expected '\\'", startPosition);
            return ParseResult.error();
        }

        char escapedChar = input.charAt(startPosition + 1);
        if (escapedChar != 'u') {
            logError("escaped-unicode-sequence", "Invalid escape sequence", startPosition);
            return ParseResult.error();
        }

        String unicodePattern = input.substring(startPosition + 2, startPosition + 2 + 4);
        char unicodeChar;
        try {
            unicodeChar = (char) Integer.parseInt(unicodePattern, 16);
        } catch (NumberFormatException e) {
            logError("escaped-unicode-sequence", "Invalid unicode hex sequence", startPosition);
            return ParseResult.error();
        }

        return ParseResult.success(String.valueOf(unicodeChar));
    }

    /**
     * "*"
     */
    private ParseResult<WildcardExpression> parseWildcardExpression(int startPosition, int endPosition) {
        return parseExpectedToken("star-expression", startPosition, endPosition, '*').mapResult(v -> new WildcardExpression());
    }

    private int charsInRange(int startPosition, int endPosition) {
        return endPosition - startPosition;
    }

    private List<Integer> findCharacters(int startPosition, int endPosition, String symbol) {
        List<Integer> results = new ArrayList<>();

        int start = startPosition;
        while (true) {
            int match = input.indexOf(symbol, start);
            if (match < 0 || match >= endPosition) {
                break;
            }
            results.add(match);
            start = match + 1;
        }

        return results;
    }

    private ParseResult<Character> parseExpectedToken(String parser, int startPosition, int endPosition, char expectedToken) {
        if (input.charAt(startPosition) != expectedToken) {
            logError(parser, "Expected '" + expectedToken + "'", startPosition);
            return ParseResult.error();
        }

        if (charsInRange(startPosition, endPosition) != 1) {
            logError(parser, "Unexpected character", startPosition + 1);
            return ParseResult.error();
        }

        return ParseResult.success(expectedToken);
    }

    private int trimLeftWhitespace(int startPosition, int endPosition) {
        while (input.charAt(startPosition) == ' ' && startPosition < endPosition - 1) {
            ++startPosition;
        }

        return startPosition;
    }

    private int trimRightWhitespace(int startPosition, int endPosition) {
        while (input.charAt(endPosition - 1) == ' ' && startPosition < endPosition - 1) {
            --endPosition;
        }

        return endPosition;
    }

    private boolean startsWith(int startPosition, char character) {
        return input.charAt(startPosition) == character;
    }

    private boolean endsWith(int endPosition, char character) {
        return input.charAt(endPosition - 1) == character;
    }

    private boolean startsAndEndsWith(int startPosition, int endPosition, char startChar, char endChar) {
        return startsWith(startPosition, startChar) && endsWith(endPosition, endChar);
    }

    private void logError(String parser, String message, int position) {
        log.debug(() -> parser + " at " + position + ": " + message);
    }
}