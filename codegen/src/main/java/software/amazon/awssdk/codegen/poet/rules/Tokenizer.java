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

package software.amazon.awssdk.codegen.poet.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import software.amazon.awssdk.utils.ToString;

/**
 * Tokenizer for string literals inside a rule set document.
 */
public class Tokenizer {
    private static final Token EOF = new Token(TokenKind.EOF, "");
    private final List<Token> tokens;
    private int index = 0;

    public Tokenizer(String source) {
        this.tokens = tokenize(source);
    }

    private static List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        TokenizerState state = new TokenizerState(source);
        do {
            Token token = next(state);
            tokens.add(token);
            if (token.type == TokenKind.EOF) {
                break;
            }
        } while (true);
        return tokens;
    }

    private static Token next(TokenizerState state) {
        if (!state.hasNext()) {
            return EOF;
        }
        char ch = state.next();
        if (ch == '{') {
            if (state.peek() == '{') {
                state.next();
                return consumeString(state, '{');
            }
            return new Token(TokenKind.OPEN_CURLY, "{");
        }
        if (ch == '}') {
            if (state.peek() == '}') {
                state.next();
                return consumeString(state, '}');
            }
            return new Token(TokenKind.CLOSE_CURLY, "}");
        }
        if (ch == '[') {
            if (state.peek() == '[') {
                state.next();
                return consumeString(state, '[');
            }
            return new Token(TokenKind.OPEN_SQUARE, "[");
        }
        if (ch == ']') {
            if (state.peek() == ']') {
                state.next();
                return consumeString(state, ']');
            }
            return new Token(TokenKind.CLOSE_SQUARE, "]");
        }
        if (ch == '#') {
            return new Token(TokenKind.HASH, "#");
        }
        if (isDigit(ch)) {
            return consumeNumber(state, ch);
        }
        if (isIdentifierStart(ch)) {
            return consumeIdentifierOrString(state, ch);
        }
        return consumeString(state, ch);
    }

    private static Token consumeNumber(TokenizerState state, char start) {
        StringBuilder buf = new StringBuilder();
        buf.append(start);
        do {
            char ch = state.peek();
            if (!isDigit(ch)) {
                break;
            }
            buf.append(state.next());
        } while (true);
        return new Token(TokenKind.NUMBER, buf.toString());
    }

    private static Token consumeIdentifierOrString(TokenizerState state, char start) {
        StringBuilder buf = new StringBuilder();
        buf.append(start);
        char ch;
        do {
            ch = state.peek();
            if (!isIdentifierPart(ch)) {
                break;
            }
            buf.append(state.next());
        } while (true);
        if (isSpecialChar(ch)) {
            return new Token(TokenKind.IDENTIFIER, buf.toString());
        }
        return consumeString(state, buf);
    }

    private static Token consumeString(TokenizerState state, char start) {
        StringBuilder buf = new StringBuilder();
        buf.append(start);
        return consumeString(state, buf);
    }

    private static Token consumeString(TokenizerState state, StringBuilder buf) {
        do {
            char ch = state.peek();
            if (isSpecialChar(ch)) {
                break;
            }
            buf.append(state.next());
        } while (true);
        return new Token(TokenKind.STRING, buf.toString());
    }

    private static boolean isSpecialChar(char ch) {
        switch (ch) {
            case 0:
            case '{':
            case '}':
            case '[':
            case ']':
            case '#':
                return true;
            default:
                return false;
        }
    }

    private static boolean isIdentifierStart(char ch) {
        return (ch >= 'a' && ch <= 'z')
               || (ch >= 'A' && ch <= 'Z')
               || (ch == '_');
    }

    private static boolean isIdentifierPart(char ch) {
        return (ch >= 'a' && ch <= 'z')
               || (ch >= 'A' && ch <= 'Z')
               || (ch >= '0' && ch <= '9')
               || (ch == '_');
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public Token peek() {
        if (index >= tokens.size()) {
            // This should never happen.
            throw new IllegalStateException("Peek called with out of bounds index");
        }
        return tokens.get(index);
    }

    public Token next() {
        if (atEof()) {
            return EOF;
        }
        Token res = tokens.get(index);
        index += 1;
        return res;
    }

    public boolean matches(TokenKind... kinds) {
        if (index + kinds.length >= tokens.size()) {
            return false;
        }
        for (int idx = 0; idx < kinds.length; idx++) {
            if (tokens.get(index + idx).type != kinds[idx]) {
                return false;
            }
        }
        return true;
    }

    // e.g., resourceId[123]
    public boolean isIndexedAccess() {
        return matches(TokenKind.IDENTIFIER, TokenKind.OPEN_SQUARE, TokenKind.NUMBER, TokenKind.CLOSE_SQUARE);
    }

    public void consumeIndexed(BiConsumer<String, Integer> consumer) {
        if (!isIndexedAccess()) {
            throw new IllegalStateException("not at indexed");
        }
        consumer.accept(tokens.get(index).value, Integer.parseInt(tokens.get(index + 2).value));
        index += 4;
    }

    // e.g., {url#scheme}
    public boolean isNamedAccess() {
        return matches(TokenKind.OPEN_CURLY, TokenKind.IDENTIFIER, TokenKind.HASH, TokenKind.IDENTIFIER, TokenKind.CLOSE_CURLY);
    }

    public void consumeNamedAccess(BiConsumer<String, String> consumer) {
        if (!isNamedAccess()) {
            throw new IllegalStateException("not at named access");
        }
        consumer.accept(tokens.get(index + 1).value, tokens.get(index + 3).value);
        index += 5;
    }

    // e.g., {Region}
    public boolean isReference() {
        return matches(TokenKind.OPEN_CURLY, TokenKind.IDENTIFIER, TokenKind.CLOSE_CURLY);
    }

    public boolean isIdentifier() {
        return matches(TokenKind.IDENTIFIER);
    }

    public void consumeIdentifier(Consumer<String> consumer) {
        if (!isIdentifier()) {
            throw new IllegalStateException("not at identifier");
        }
        consumer.accept(tokens.get(index).value);
        index += 1;
    }

    public void expectAtEof(String state) {
        if (!atEof()) {
            throw new IllegalArgumentException(
                String.format("unexpected extra tokens while parsing %s, starting at: %s", state, peek()));
        }
    }

    public void consumeReferenceAccess(Consumer<String> consumer) {
        if (!isReference()) {
            throw new IllegalStateException("not at reference expression");
        }
        consumer.accept(tokens.get(index + 1).value);
        index += 3;
    }

    public boolean atEof() {
        return index >= tokens.size() - 1;
    }

    enum TokenKind {
        STRING,
        NUMBER,
        IDENTIFIER,
        HASH,
        OPEN_CURLY,
        CLOSE_CURLY,
        OPEN_SQUARE,
        CLOSE_SQUARE,
        EOF,
    }

    static class TokenizerState {
        private final String source;
        private int index = 0;

        TokenizerState(String source) {
            this.source = source;
        }

        public char peek() {
            if (index == source.length()) {
                return 0;
            }
            return source.charAt(index);
        }

        public boolean hasNext() {
            return index < source.length();
        }

        public char next() {
            if (index == source.length()) {
                return 0;
            }
            char res = source.charAt(index);
            index += 1;
            return res;
        }
    }

    static class Token {
        private final TokenKind type;
        private final String value;

        Token(TokenKind type, String value) {
            this.type = type;
            this.value = value;
        }

        public TokenKind type() {
            return type;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return ToString.builder("Token")
                           .add("type", type)
                           .add("value", value)
                           .build();
        }
    }
}
