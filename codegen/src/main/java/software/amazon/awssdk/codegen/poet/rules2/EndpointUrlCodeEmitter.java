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

package software.amazon.awssdk.codegen.poet.rules2;

import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.endpoints.EndpointUrl;

/**
 * Emits the optimal {@code EndpointUrl} construction code for a URL expression.
 *
 * <p>This class encapsulates both the analysis of the URL expression structure (determining whether
 * it can be statically decomposed) and the code emission. When the URL starts with a literal scheme
 * prefix ({@code https://} or {@code http://}) and the components (scheme, host, port, path) can be
 * identified at codegen time then we eliminate runtime parsing and use {@code EndpointUrl.fromComponents()}.
 * Otherwise, falls back to {@code EndpointUrl.fromString()} for runtime parsing.
 *
 * <p>The endpoint URL spec guarantees that URLs contain only scheme, host, optional port, and optional
 * base path (no query or fragment). Dynamic (template based) resolution are only supported in the host
 * and path segments in pre-parsing.  Otherwise, we fall back to runtime parsing.
 */
final class EndpointUrlCodeEmitter {

    private static final String HTTPS_SCHEME_PREFIX = "https://";
    private static final String HTTP_SCHEME_PREFIX = "http://";

    private EndpointUrlCodeEmitter() {
    }

    /**
     * Emit the optimal EndpointUrl construction code for the given URL expression.
     *
     * <p>Writes to {@code builder} either:
     * <ul>
     *   <li>{@code EndpointUrl.fromComponents(scheme, hostExpr, port, pathExpr)} when the URL
     *       can be statically decomposed, or</li>
     *   <li>{@code EndpointUrl.fromString(urlExpr)} as a fallback.</li>
     * </ul>
     *
     * @param urlExpr the URL expression from the endpoint rule
     * @param builder the CodeBlock builder to emit into
     * @param codegenVisitor the parent code generator, used to emit sub-expressions
     */
    static void emit(RuleExpression urlExpr, CodeBlock.Builder builder, CodeGeneratorVisitor codegenVisitor) {
        if (urlExpr instanceof LiteralStringExpression) {
            emitFromLiteralString(((LiteralStringExpression) urlExpr).value(), builder);
            return;
        }
        if (urlExpr instanceof StringConcatExpression) {
            emitFromStringConcat((StringConcatExpression) urlExpr, builder, codegenVisitor);
            return;
        }
        // Expression type we can't decompose (e.g. variable reference, function call)
        emitRuntimeParse(urlExpr, builder, codegenVisitor);
    }

    /**
     * Emit a fully static URL as EndpointUrl.fromComponents() with all literal arguments.
     */
    private static void emitFromLiteralString(String url, CodeBlock.Builder builder) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            // No scheme found — shouldn't happen per spec, but fall back safely
            builder.add("$T.fromString($S)", EndpointUrl.class, url);
            return;
        }

        String scheme = url.substring(0, schemeEnd);
        int authorityStart = schemeEnd + 3;
        int pathStart = url.indexOf('/', authorityStart);

        String authority;
        String encodedPath;
        if (pathStart < 0) {
            authority = url.substring(authorityStart);
            encodedPath = "";
        } else {
            authority = url.substring(authorityStart, pathStart);
            encodedPath = url.substring(pathStart);
        }

        int port = -1;
        String host;
        int colonPos = authority.lastIndexOf(':');
        if (colonPos >= 0) {
            try {
                port = Integer.parseInt(authority.substring(colonPos + 1));
                host = authority.substring(0, colonPos);
            } catch (NumberFormatException e) {
                // Not a valid port — can't reliably decompose, fall back to runtime parsing
                builder.add("$T.fromString($S)", EndpointUrl.class, url);
                return;
            }
        } else {
            host = authority;
        }

        builder.add("$T.fromComponents($S, $S, $L, $S)", EndpointUrl.class, scheme, host, port, encodedPath);
    }

    /**
     * Emit code for a StringConcatExpression URL template. If the URL can be statically decomposed
     * (starts with a literal scheme prefix), emits {@code EndpointUrl.fromComponents()}. Otherwise,
     * falls back to {@code EndpointUrl.fromString()} for runtime parsing.
     *
     * <p> StringConcatExpressions are created by our codegen pre-parser: {@link ExpressionParser}.
     *  It splits on "{...}" boundaries, producing alternating literal strings and variable/member-access references. The
     *  result is a StringConcatExpression whose expressions() list interleaves:
     *  <ul>
     *      <li>LiteralStringExpression — static text fragments</li>
     *      <li>VariableReferenceExpression — "{Region}" eg a variable reference to "Region"</li>
     *      <li>MemberAccessExpression — "{PartitionResult#dnsSuffix}" → member access on variable "PartitionResult"</li>
     *  </ul>
     *
     *  <p>Typical service endpoints follow something like: "https://sts.{Region}.{PartitionResult#dnsSuffix}" which
     *  parses to something like:
     * {@snippet :
     * StringConcatExpression([
     *     LiteralStringExpression("https://sts."),
     *     VariableReferenceExpression("Region"),
     *     LiteralStringExpression("."),
     *     MemberAccessExpression(source=VarRef("PartitionResult"), name="dnsSuffix")
     * ])
     * }
     *
     */
    private static void emitFromStringConcat(StringConcatExpression concatExpr,
                                             CodeBlock.Builder builder,
                                             CodeGeneratorVisitor codegenVisitor) {
        List<RuleExpression> expressions = concatExpr.expressions();
        if (expressions.isEmpty()) {
            emitRuntimeParse(concatExpr, builder, codegenVisitor);
            return;
        }

        // The first expression must be a literal starting with a scheme to preparse
        RuleExpression firstExpr = expressions.get(0);
        if (!(firstExpr instanceof LiteralStringExpression)) {
            emitRuntimeParse(concatExpr, builder, codegenVisitor);
            return;
        }

        String firstLiteral = ((LiteralStringExpression) firstExpr).value();
        String scheme;
        String hostStartStr;
        if (firstLiteral.startsWith(HTTPS_SCHEME_PREFIX)) {
            scheme = "https";
            hostStartStr = firstLiteral.substring(HTTPS_SCHEME_PREFIX.length());
        } else if (firstLiteral.startsWith(HTTP_SCHEME_PREFIX)) {
            scheme = "http";
            hostStartStr = firstLiteral.substring(HTTP_SCHEME_PREFIX.length());
        } else {
            emitRuntimeParse(concatExpr, builder, codegenVisitor);
            return;
        }

        // Scan expressions to find port and path boundaries, collecting host and path parts
        int port = -1;
        List<RuleExpression> hostParts = new ArrayList<>();
        List<RuleExpression> pathParts = new ArrayList<>();
        boolean foundPath = false;

        // Process the remainder of the first literal (after "scheme://")
        if (!hostStartStr.isEmpty()) {
            ScanResult scanResult = scanLiteral(hostStartStr, hostParts, pathParts);
            if (!scanResult.parseable) {
                emitRuntimeParse(concatExpr, builder, codegenVisitor);
                return;
            }
            if (scanResult.foundPort) {
                port = scanResult.port;
            }
            foundPath = scanResult.foundPath;
        }

        // Process remaining expressions
        for (int i = 1; i < expressions.size(); i++) {
            RuleExpression expr = expressions.get(i);

            if (foundPath) {
                // Everything after the path boundary is path — literals and expressions alike
                pathParts.add(expr);
                continue;
            }

            if (expr instanceof LiteralStringExpression) {
                String literal = ((LiteralStringExpression) expr).value();
                ScanResult scanResult = scanLiteral(literal, hostParts, pathParts);
                if (!scanResult.parseable) {
                    emitRuntimeParse(concatExpr, builder, codegenVisitor);
                    return;
                }
                if (scanResult.foundPort) {
                    port = scanResult.port;
                }
                foundPath = scanResult.foundPath;
            } else {
                // Non-literal expression — part of the host since we haven't hit a path
                hostParts.add(expr);
            }
        }

        // Emit: EndpointUrl.fromComponents(scheme, hostExpr, port, pathExpr)
        builder.add("$T.fromComponents($S, ", EndpointUrl.class, scheme);
        emitConcatExpression(hostParts, builder, codegenVisitor);
        builder.add(", $L, ", port);
        emitConcatExpression(pathParts, builder, codegenVisitor);
        builder.add(")");
    }

    /**
     * Emit EndpointUrl.fromString(urlExpr) for runtime parsing when static decomposition isn't possible.
     */
    private static void emitRuntimeParse(RuleExpression urlExpr, CodeBlock.Builder builder,
                                         CodeGeneratorVisitor codegenVisitor) {
        builder.add("$T.fromString(", EndpointUrl.class);
        urlExpr.accept(codegenVisitor);
        builder.add(")");
    }

    /**
     * Emit a list of expression parts as a concatenated expression.
     *
     * <p>If the list is empty, emits an empty string literal. Otherwise, wraps them in a
     * {@link StringConcatExpression} and delegates to the code generator's existing concat
     * emission logic.
     */
    private static void emitConcatExpression(List<RuleExpression> parts, CodeBlock.Builder builder,
                                             CodeGeneratorVisitor codegenVisitor) {
        if (parts.isEmpty()) {
            builder.add("$S", "");
            return;
        }
        StringConcatExpression.Builder concatBuilder = StringConcatExpression.builder();
        for (RuleExpression part : parts) {
            concatBuilder.addExpression(part);
        }
        concatBuilder.build().accept(codegenVisitor);
    }

    /**
     * Scan a literal string segment for port (:{digits}) and path (/) boundaries.
     *
     * <p>This method mutates {@code hostParts} and {@code pathParts} as a side effect, appending
     * the relevant portions of the literal to each list. On a {@code NOT_PARSEABLE} result, the
     * lists may have been partially modified — callers must not reuse the lists after a failure.
     */
    private static ScanResult scanLiteral(String literal,
                                          List<RuleExpression> hostParts,
                                          List<RuleExpression> pathParts) {
        int slashIdx = literal.indexOf('/');
        int portColonIdx = findPortColon(literal);

        if (slashIdx >= 0 && (portColonIdx < 0 || slashIdx < portColonIdx)) {
            // Path found before any port in this literal
            String hostPart = literal.substring(0, slashIdx);
            if (!hostPart.isEmpty()) {
                hostParts.add(new LiteralStringExpression(hostPart));
            }
            pathParts.add(new LiteralStringExpression(literal.substring(slashIdx)));
            return ScanResult.pathFound();
        }
        if (portColonIdx >= 0) {
            // Port found
            String hostPart = literal.substring(0, portColonIdx);
            if (!hostPart.isEmpty()) {
                hostParts.add(new LiteralStringExpression(hostPart));
            }
            String portAndRest = literal.substring(portColonIdx + 1);
            int restSlashIdx = portAndRest.indexOf('/');
            int port;
            if (restSlashIdx >= 0) {
                try {
                    port = Integer.parseInt(portAndRest.substring(0, restSlashIdx));
                } catch (NumberFormatException e) {
                    return ScanResult.NOT_PARSEABLE;
                }
                pathParts.add(new LiteralStringExpression(portAndRest.substring(restSlashIdx)));
                return ScanResult.portAndPathFound(port);
            } else {
                try {
                    port = Integer.parseInt(portAndRest);
                } catch (NumberFormatException e) {
                    return ScanResult.NOT_PARSEABLE;
                }
                return ScanResult.portFound(port);
            }
        } else {
            // No port or path — this literal is part of the host
            hostParts.add(new LiteralStringExpression(literal));
            return ScanResult.HOST_CONTINUE;
        }
    }

    /**
     * Find the index of a port colon in a literal string. A port colon is a ':' followed by
     * one or more digits (optionally followed by '/' or end of string).
     */
    private static int findPortColon(String literal) {
        int idx = literal.indexOf(':');
        while (idx >= 0) {
            int digitStart = idx + 1;
            if (digitStart < literal.length() && isDigit(literal.charAt(digitStart))) {
                int digitEnd = digitStart;
                while (digitEnd < literal.length() && isDigit(literal.charAt(digitEnd))) {
                    digitEnd++;
                }
                if (digitEnd == literal.length() || literal.charAt(digitEnd) == '/') {
                    return idx;
                }
            }
            idx = literal.indexOf(':', idx + 1);
        }
        return -1;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Internal result type for the literal scanning step.
     */
    private static final class ScanResult {
        private static final ScanResult NOT_PARSEABLE = new ScanResult(false, false, false, -1);
        private static final ScanResult HOST_CONTINUE = new ScanResult(true, false, false, -1);

        private final boolean parseable;
        private final boolean foundPort;
        private final boolean foundPath;
        private final int port;

        private ScanResult(boolean parseable, boolean foundPort, boolean foundPath, int port) {
            this.parseable = parseable;
            this.foundPort = foundPort;
            this.foundPath = foundPath;
            this.port = port;
        }

        static ScanResult pathFound() {
            return new ScanResult(true, false, true, -1);
        }

        static ScanResult portFound(int port) {
            return new ScanResult(true, true, false, port);
        }

        static ScanResult portAndPathFound(int port) {
            return new ScanResult(true, true, true, port);
        }
    }
}
