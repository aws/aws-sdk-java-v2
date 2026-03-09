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

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes an endpoint URL expression at codegen time to determine if its components
 * (scheme, host, port, path) can be statically decomposed, enabling the use of
 * {@code EndpointUrl.of()} instead of {@code EndpointUrl.parse()} in generated code.
 */
final class EndpointUrlCodegenAnalyzer {

    private static final String HTTPS_SCHEME_PREFIX = "https://";
    private static final String HTTP_SCHEME_PREFIX = "http://";

    private EndpointUrlCodegenAnalyzer() {
    }

    /**
     * Analyze the URL expression from an EndpointExpression.
     *
     * @param urlExpr the URL expression (typically a StringConcatExpression or LiteralStringExpression)
     * @return an AnalysisResult indicating whether pre-parsing is possible and the decomposed components
     */
    static AnalysisResult analyze(RuleExpression urlExpr) {
        if (urlExpr instanceof LiteralStringExpression) {
            return analyzeStaticUrl(((LiteralStringExpression) urlExpr).value());
        }
        if (!(urlExpr instanceof StringConcatExpression)) {
            return AnalysisResult.notPreParseable();
        }
        return analyzeStringConcat((StringConcatExpression) urlExpr);
    }

    /**
     * Analyze a fully static URL string (from a LiteralStringExpression).
     */
    private static AnalysisResult analyzeStaticUrl(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return AnalysisResult.notPreParseable();
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
                // Not a valid port — treat the whole authority as the host
                host = authority;
            }
        } else {
            host = authority;
        }

        RuleExpression hostExpr = new LiteralStringExpression(host);
        return new AnalysisResult(true, scheme, hostExpr, port, encodedPath);
    }

    /**
     * Analyze a StringConcatExpression URL template.
     */
    private static AnalysisResult analyzeStringConcat(StringConcatExpression concatExpr) {
        List<RuleExpression> expressions = concatExpr.expressions();
        if (expressions.isEmpty()) {
            return AnalysisResult.notPreParseable();
        }

        // Step 1: The first expression must be a literal starting with a scheme
        RuleExpression firstExpr = expressions.get(0);
        if (!(firstExpr instanceof LiteralStringExpression)) {
            return AnalysisResult.notPreParseable();
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
            return AnalysisResult.notPreParseable();
        }

        // Step 2: Scan expressions to find port and path boundaries
        int port = -1;
        StringBuilder encodedPathBuilder = new StringBuilder();
        List<RuleExpression> hostExpressions = new ArrayList<>();
        boolean foundPath = false;

        // Add the remainder of the first literal (after "scheme://") as the start of host
        if (!hostStartStr.isEmpty()) {
            ScanResult scanResult = scanLiteral(hostStartStr, hostExpressions, encodedPathBuilder);
            if (!scanResult.preParseable) {
                return AnalysisResult.notPreParseable();
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
                // Once we're in the path, any non-literal means path is dynamic → not pre-parseable
                if (!(expr instanceof LiteralStringExpression)) {
                    return AnalysisResult.notPreParseable();
                }
                encodedPathBuilder.append(((LiteralStringExpression) expr).value());
                continue;
            }

            if (expr instanceof LiteralStringExpression) {
                String literal = ((LiteralStringExpression) expr).value();
                ScanResult scanResult = scanLiteral(literal, hostExpressions, encodedPathBuilder);
                if (!scanResult.preParseable) {
                    return AnalysisResult.notPreParseable();
                }
                if (scanResult.foundPort) {
                    port = scanResult.port;
                }
                foundPath = scanResult.foundPath;
            } else {
                // Non-literal expression (variable reference, function call, etc.)
                // This is part of the host as long as we haven't hit a path
                hostExpressions.add(expr);
            }
        }

        // Build the host expression from collected parts
        RuleExpression hostExpr = buildHostExpression(hostExpressions);
        String encodedPath = encodedPathBuilder.toString();

        return new AnalysisResult(true, scheme, hostExpr, port, encodedPath);
    }

    /**
     * Scan a literal string segment for port (:{digits}) and path (/) boundaries.
     * Adds host portions to hostExpressions and path portions to encodedPathBuilder.
     *
     * @return a ScanResult indicating what was found
     */
    private static ScanResult scanLiteral(String literal,
                                          List<RuleExpression> hostExpressions,
                                          StringBuilder encodedPathBuilder) {
        int slashIdx = literal.indexOf('/');
        int portColonIdx = findPortColon(literal);

        if (slashIdx >= 0 && (portColonIdx < 0 || slashIdx < portColonIdx)) {
            // Path found before any port in this literal
            String hostPart = literal.substring(0, slashIdx);
            if (!hostPart.isEmpty()) {
                hostExpressions.add(new LiteralStringExpression(hostPart));
            }
            encodedPathBuilder.append(literal.substring(slashIdx));
            return ScanResult.pathFound();
        } else if (portColonIdx >= 0) {
            // Port found
            String hostPart = literal.substring(0, portColonIdx);
            if (!hostPart.isEmpty()) {
                hostExpressions.add(new LiteralStringExpression(hostPart));
            }
            String portAndRest = literal.substring(portColonIdx + 1);
            int restSlashIdx = portAndRest.indexOf('/');
            int port;
            if (restSlashIdx >= 0) {
                try {
                    port = Integer.parseInt(portAndRest.substring(0, restSlashIdx));
                } catch (NumberFormatException e) {
                    return ScanResult.notPreParseable();
                }
                encodedPathBuilder.append(portAndRest.substring(restSlashIdx));
                return ScanResult.portAndPathFound(port);
            } else {
                try {
                    port = Integer.parseInt(portAndRest);
                } catch (NumberFormatException e) {
                    return ScanResult.notPreParseable();
                }
                return ScanResult.portFound(port);
            }
        } else {
            // No port or path in this literal — it's part of the host
            hostExpressions.add(new LiteralStringExpression(literal));
            return ScanResult.hostContinue();
        }
    }

    /**
     * Find the index of a port colon in a literal string. A port colon is a ':' followed by
     * one or more digits (optionally followed by '/' or end of string).
     *
     * @return the index of the colon, or -1 if no port pattern is found
     */
    private static int findPortColon(String literal) {
        int idx = literal.indexOf(':');
        while (idx >= 0) {
            // Check if what follows the colon is all digits (until '/' or end)
            int digitStart = idx + 1;
            if (digitStart < literal.length() && isDigit(literal.charAt(digitStart))) {
                int digitEnd = digitStart;
                while (digitEnd < literal.length() && isDigit(literal.charAt(digitEnd))) {
                    digitEnd++;
                }
                // Valid port if we consumed at least one digit and hit end or '/'
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
     * Build a single RuleExpression representing the host from a list of host component expressions.
     */
    private static RuleExpression buildHostExpression(List<RuleExpression> hostExpressions) {
        if (hostExpressions.isEmpty()) {
            return new LiteralStringExpression("");
        }
        if (hostExpressions.size() == 1) {
            return hostExpressions.get(0);
        }
        StringConcatExpression.Builder builder = StringConcatExpression.builder();
        for (RuleExpression expr : hostExpressions) {
            builder.addExpression(expr);
        }
        return builder.build();
    }

    /**
     * Internal result type for the literal scanning step.
     */
    private static final class ScanResult {
        private static final ScanResult NOT_PRE_PARSEABLE = new ScanResult(false, false, false, -1);
        private static final ScanResult HOST_CONTINUE = new ScanResult(true, false, false, -1);

        private final boolean preParseable;
        private final boolean foundPort;
        private final boolean foundPath;
        private final int port;

        private ScanResult(boolean preParseable, boolean foundPort, boolean foundPath, int port) {
            this.preParseable = preParseable;
            this.foundPort = foundPort;
            this.foundPath = foundPath;
            this.port = port;
        }

        static ScanResult notPreParseable() {
            return NOT_PRE_PARSEABLE;
        }

        static ScanResult hostContinue() {
            return HOST_CONTINUE;
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

    /**
     * Result of analyzing a URL expression.
     */
    static final class AnalysisResult {
        private static final AnalysisResult NOT_PRE_PARSEABLE = new AnalysisResult(false, null, null, -1, null);

        private final boolean preParseable;
        private final String scheme;
        private final RuleExpression hostExpr;
        private final int port;
        private final String encodedPath;

        private AnalysisResult(boolean preParseable, String scheme, RuleExpression hostExpr,
                               int port, String encodedPath) {
            this.preParseable = preParseable;
            this.scheme = scheme;
            this.hostExpr = hostExpr;
            this.port = port;
            this.encodedPath = encodedPath;
        }

        static AnalysisResult notPreParseable() {
            return NOT_PRE_PARSEABLE;
        }

        boolean isPreParseable() {
            return preParseable;
        }

        String scheme() {
            return scheme;
        }

        RuleExpression hostExpr() {
            return hostExpr;
        }

        int port() {
            return port;
        }

        String encodedPath() {
            return encodedPath;
        }
    }
}
