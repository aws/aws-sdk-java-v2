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

package software.amazon.awssdk.buildtools.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Verify that we're not using classes in rt.jar that aren't exported via the java.base module.
 *
 * If anything fails this check, it increases our module dependencies. If you absolutely must use one of these
 * (e.g. java.beans) because it's fundamental to your functionality, you can suppress this checkstyle rule via
 * {@link #setLegalPackages(String...)}, but know that it is not free - you're essentially adding a dependency
 * for customers that use the module path.
 */
public class NonJavaBaseModuleCheck extends AbstractCheck {
    private static final List<String> ILLEGAL_PACKAGES = Arrays.asList(
        "java",
        "javax",
        "sun",
        "apple",
        "com.apple",
        "com.oracle");

    private static final Set<String> LEGAL_PACKAGES = new HashSet<>(Arrays.asList(
        "java.io",
        "java.lang",
        "java.lang.annotation",
        "java.lang.invoke",
        "java.lang.module",
        "java.lang.ref",
        "java.lang.reflect",
        "java.math",
        "java.net",
        "java.net.spi",
        "java.nio",
        "java.nio.channels",
        "java.nio.channels.spi",
        "java.nio.charset",
        "java.nio.charset.spi",
        "java.nio.file",
        "java.nio.file.attribute",
        "java.nio.file.spi",
        "java.security",
        "java.security.acl",
        "java.security.cert",
        "java.security.interfaces",
        "java.security.spec",
        "java.text",
        "java.text.spi",
        "java.time",
        "java.time.chrono",
        "java.time.format",
        "java.time.temporal",
        "java.time.zone",
        "java.util",
        "java.util.concurrent",
        "java.util.concurrent.atomic",
        "java.util.concurrent.locks",
        "java.util.function",
        "java.util.jar",
        "java.util.regex",
        "java.util.spi",
        "java.util.stream",
        "java.util.jar",
        "java.util.zip",
        "javax.crypto",
        "javax.crypto.interfaces",
        "javax.crypto.spec",
        "javax.net",
        "javax.net.ssl",
        "javax.security.auth",
        "javax.security.auth.callback",
        "javax.security.auth.login",
        "javax.security.auth.spi",
        "javax.security.auth.x500",
        "javax.security.cert"));

    private static final Pattern CLASSNAME_START_PATTERN = Pattern.compile("[A-Z]");

    private String currentSdkPackage;

    private HashMap<String, Set<String>> additionalLegalPackagesBySdkPackage = new HashMap<>();

    /**
     * Additional legal packages are formatted as "sdk.package.name:jdk.package.name,sdk.package.name2:jdk.package.name2".
     * Multiple SDK packages can be repeated.
     */
    public void setLegalPackages(String... legalPackages) {
        for (String additionalLegalPackage : legalPackages) {
            String[] splitPackage = additionalLegalPackage.split(":", 2);
            if (splitPackage.length != 2) {
                throw new IllegalArgumentException("Invalid legal package definition '" + additionalLegalPackage + "'. Expected"
                                                   + " format is sdk.package.name:jdk.package.name");
            }

            this.additionalLegalPackagesBySdkPackage.computeIfAbsent(splitPackage[0], k -> new HashSet<>())
                                                    .add(splitPackage[1]);
        }
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] { TokenTypes.IMPORT, TokenTypes.STATIC_IMPORT, TokenTypes.PACKAGE_DEF };
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (ast.getType() == TokenTypes.PACKAGE_DEF) {
            handlePackageDefToken(ast);
            return;
        }

        handleImportToken(ast);
    }

    private void handlePackageDefToken(DetailAST ast) {
        this.currentSdkPackage = FullIdent.createFullIdent(ast.getLastChild().getPreviousSibling()).getText();
    }

    private void handleImportToken(DetailAST ast) {
        FullIdent importIdentifier;
        if (ast.getType() == TokenTypes.IMPORT) {
            importIdentifier = FullIdent.createFullIdentBelow(ast);
        } else {
            importIdentifier = FullIdent.createFullIdent(ast.getFirstChild().getNextSibling());
        }

        String importText = importIdentifier.getText();
        if (isIllegalImport(importText) && !isLegalImport(importText)) {
            log(ast, "Import '" + importText + "' uses a JDK class that is not in java.base. This essentially adds an "
                     + "additional module dependency. Don't suppress this rule unless it's absolutely required, and only "
                     + "suppress the specific package you need via checkstyle.xml instead of suppressing the entire rule.");
        }
    }

    private boolean isIllegalImport(String importText) {
        for (String illegalPackage : ILLEGAL_PACKAGES) {
            if (importText.startsWith(illegalPackage + ".")) {
                return true;
            }
        }

        return false;
    }

    private boolean isLegalImport(String importText) {
        String importPackageWithTrailingDot = CLASSNAME_START_PATTERN.split(importText, 2)[0];
        String importPackage = importText.substring(0, importPackageWithTrailingDot.length() - 1);

        if (LEGAL_PACKAGES.contains(importPackage)) {
            return true;
        }

        if (additionalLegalPackagesBySdkPackage.entrySet()
                                               .stream()
                                               .anyMatch(e -> currentSdkPackage.startsWith(e.getKey()) &&
                                                              e.getValue().contains(importPackage))) {
            return true;
        }

        return false;
    }
}
