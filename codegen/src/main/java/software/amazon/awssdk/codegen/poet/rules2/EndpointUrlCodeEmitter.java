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
 * prefix ({@code https://} or {@code http://}), the components (scheme, host, port, path) can be
 * identified at codegen time and emitted as {@code EndpointUrl.fromComponents()} — eliminating all
 * runtime string parsing. Otherwise, falls back to {@code EndpointUrl.fromString()}.
 *
 * <p>The endpoint URL spec guarantees that URLs contain only scheme, host, optional port, and optional
 * base path (no query or fragment). This means any expression appearing after the path boundary is
 * still part of the path component, allowing dynamic paths to be pre-parsed as well.
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
            emitStaticUrl(((LiteralStringExpression) urlExpr).value(), builder);
            return;
        }
        if (urlExpr instanceof StringConcatExpression) {
            if (tryEmitFromComponents((StringConcatExpression) urlExpr, builder, codegenVisitor)) {
                return;
            }
        }
        // Fallback: emit EndpointUrl.fromString(fullUrl)
        emitFromString(urlExpr, builder, codegenVisitor);
    }

    /**
     * Emit a fully static URL as EndpointUrl.fromComponents() with all literal arguments.
     */
    private static void emitStaticUrl(String url, CodeBlock.Builder builder) {
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
            host = authority.substring(0, colonPos);
            try {
                port = Integer.parseInt(authority.substring(colonPos + 1));
            } catch (NumberFormatException e) {
                host = authority;
            }
        } else {
            host = authority;
        }

        builder.add("$T.fromComponents($S, $S, $L, $S)", EndpointUrl.class, scheme, host, port, encodedPath);
    }

    /**
     * Attempt to emit EndpointUrl.fromComponents() for a StringConcatExpression URL template.
     *
     * <p>Returns {@code true} if successful (the URL could be decomposed), {@code false} if the
     * caller should fall back to fromString().
     */
    private static boolean tryEmitFromComponents(StringConcatExpression concatExpr,
                                                 CodeBlock.Builder builder,
                                                 CodeGeneratorVisitor codegenVisitor) {
        List<RuleExpression> expressions = concatExpr.expressions();
        if (expressions.isEmpty()) {
            return false;
        }

        // The first expression must be a literal starting with a scheme
        RuleExpression firstExpr = expressions.get(0);
        if (!(firstExpr instanceof LiteralStringExpression)) {
            return false;
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
            return false;
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
                return false;
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
                if (expr instanceof LiteralStringExpression) {
                    pathParts.add(expr);
                } else {
                    pathParts.add(expr);
                }
                continue;
            }

            if (expr instanceof LiteralStringExpression) {
                String literal = ((LiteralStringExpression) expr).value();
                ScanResult scanResult = scanLiteral(literal, hostParts, pathParts);
                if (!scanResult.parseable) {
                    return false;
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
        return true;
    }

    /**
     * Emit EndpointUrl.fromString(urlExpr) as the fallback.
     */
    private static void emitFromString(RuleExpression urlExpr, CodeBlock.Builder builder,
                                       CodeGeneratorVisitor codegenVisitor) {
        builder.add("$T.fromString(", EndpointUrl.class);
        urlExpr.accept(codegenVisitor);
        builder.add(")");
    }

    /**
     * Emit a list of expression parts as a concatenated expression.
     *
     * <p>If the list is empty, emits an empty string literal. If it contains a single literal string,
     * emits that literal directly. Otherwise, emits the parts joined with {@code +}.
     */
    private static void emitConcatExpression(List<RuleExpression> parts, CodeBlock.Builder builder,
                                             CodeGeneratorVisitor codegenVisitor) {
        if (parts.isEmpty()) {
            builder.add("$S", "");
            return;
        }
        if (parts.size() == 1 && parts.get(0) instanceof LiteralStringExpression) {
            builder.add("$S", ((LiteralStringExpression) parts.get(0)).value());
            return;
        }
        boolean isFirst = true;
        for (RuleExpression part : parts) {
            if (!isFirst) {
                builder.add(" + ");
            }
            part.accept(codegenVisitor);
            isFirst = false;
        }
    }

    /**
     * Scan a literal string segment for port (:{digits}) and path (/) boundaries.
     * Adds host portions to hostParts and path portions to pathParts.
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
        } else if (portColonIdx >= 0) {
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
