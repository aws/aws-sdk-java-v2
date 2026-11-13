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

package software.amazon.awssdk.codegen.emitters;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes unused and duplicate imports from generated Java source.
 * <p>
 * An import is unused when its simple name (the part after the last {@code .}) does not appear as a whole word anywhere
 * in the file outside the import lines. Comments, Javadoc and string literals count, so an import whose name only
 * appears in a {@code {@link}} is kept. Wildcard imports are never removed. An import that appears more than once is
 * removed once.
 */
public class UnusedImportRemover implements CodeTransformer {
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import(?:\\s+)(?:static\\s+)?(.*)(?:\\s*);",
                                                            Pattern.MULTILINE);
    private static final Pattern WORD = Pattern.compile("\\b\\w+\\b");
    private static final Pattern IDENTIFIER = Pattern.compile("\\w+");
    private static final Pattern REGEX_SAFE_IMPORT = Pattern.compile("[\\w.$]*");
    private static final String IMPORT_KEYWORD = "import";

    @Override
    public String apply(String content) {
        return findUnusedImports(content).stream().map(this::removeImportFunction).reduce(Function.identity(),
                Function::andThen).apply(content);
    }

    private Function<String, String> removeImportFunction(String importToRemove) {
        return c -> c.replaceFirst(findSpecificImportRegex(importToRemove), "");
    }

    private Set<String> findUnusedImports(String content) {
        return findImports(content).stream().filter(isUnused(content)).collect(toSet());
    }

    private List<String> findImports(String content) {
        Matcher m = IMPORT_PATTERN.matcher(content);
        List<String> imports = new ArrayList<>();
        while (m.find()) {
            imports.add(m.group(1));
        }
        return imports;
    }

    private String removeAllImports(String content) {
        Matcher matcher = IMPORT_PATTERN.matcher(content);
        return matcher.replaceAll("");
    }

    private Predicate<String> isUnused(String content) {
        String contentWithoutImports = removeAllImports(content);
        Set<String> words = findWords(contentWithoutImports);
        int[] importKeywordPositions = findImportKeywordPositions(content);
        return importToCheck -> !importToCheck.contains("*")
                && (isNotReferenced(words, contentWithoutImports, importToCheck)
                    || isDuplicate(content, importKeywordPositions, importToCheck));
    }

    private static Set<String> findWords(String contentWithoutImports) {
        Set<String> words = new HashSet<>();
        Matcher m = WORD.matcher(contentWithoutImports);
        while (m.find()) {
            words.add(m.group());
        }
        return words;
    }

    private static int[] findImportKeywordPositions(String content) {
        List<Integer> positions = new ArrayList<>();
        for (int i = content.indexOf(IMPORT_KEYWORD); i >= 0; i = content.indexOf(IMPORT_KEYWORD, i + 1)) {
            positions.add(i);
        }
        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean isNotReferenced(Set<String> words, String contentWithoutImports, String importToCheck) {
        String symbol = importToCheck.substring(importToCheck.lastIndexOf('.') + 1);
        if (IDENTIFIER.matcher(symbol).matches()) {
            // Same answer as the regex below, without a scan per import: a "\b<symbol>\b" match is a whole run of
            // \w characters with a \b on each side, and the "\b\w+\b" scan in findWords returns exactly those runs.
            // This holds for both the JDK 8-18 and the JDK 19+ definitions of \b.
            return !words.contains(symbol);
        }
        return !Pattern.compile(String.format("\\b%s\\b", symbol)).matcher(contentWithoutImports).find();
    }

    private boolean isDuplicate(String content, int[] importKeywordPositions, String importToCheck) {
        Matcher m = Pattern.compile(findSpecificImportRegex(importToCheck)).matcher(content);
        if (!REGEX_SAFE_IMPORT.matcher(importToCheck).matches()) {
            return m.find() && m.find();
        }
        // Same answer as "m.find() && m.find()", trying only the positions where "import" occurs: when the import
        // contains only word characters, '.' and '$', every match of the regex starts with the literal "import".
        m.useAnchoringBounds(false).useTransparentBounds(true);
        int count = 0;
        int nextStart = 0;
        for (int position : importKeywordPositions) {
            if (position < nextStart) {
                continue;
            }
            m.region(position, content.length());
            if (m.lookingAt()) {
                if (++count == 2) {
                    return true;
                }
                nextStart = m.end();
            }
        }
        return false;
    }

    private String findSpecificImportRegex(String specificImport) {
        return String.format("import(?:\\s+)(?:static )?(?:%s)(?:\\s*);", specificImport);
    }
}
