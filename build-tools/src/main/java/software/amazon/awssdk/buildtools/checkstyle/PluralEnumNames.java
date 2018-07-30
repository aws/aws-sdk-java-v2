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

package software.amazon.awssdk.buildtools.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.Arrays;
import java.util.List;

/**
 * A rule that disallows plurality in enum names (eg. it should be "Property.MY_PROPERTY", not "Properties.MY_PROPERTY"). This
 * also applies the validation to "pseudo-enums", classes with public static fields.
 */
public class PluralEnumNames extends AbstractCheck {
    private static final List<String> PLURAL_ENDINGS = Arrays.asList("S", "s");
    private static final List<String> NON_PLURAL_ENDINGS = Arrays.asList("Status");

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] { TokenTypes.CLASS_DEF, TokenTypes.ENUM_DEF };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public void visitToken(DetailAST ast) {
        String classOrEnumName = ast.findFirstToken(TokenTypes.IDENT).getText();

        if (isPlural(classOrEnumName) && isPluralDisallowed(ast)) {
            log(ast, "Enum or class name should be singular, not plural (Property.VALUE not Properties.VALUE).");
        }
    }

    private boolean isPlural(String className) {
        return PLURAL_ENDINGS.stream().anyMatch(className::endsWith) &&
               NON_PLURAL_ENDINGS.stream().noneMatch(className::endsWith);
    }

    private boolean isPluralDisallowed(DetailAST ast) {
        return isEnum(ast) || hasPublicStaticField(ast);
    }

    private boolean isEnum(DetailAST ast) {
        return ast.getType() == TokenTypes.ENUM_DEF;
    }

    private boolean hasPublicStaticField(DetailAST ast) {
        DetailAST classBody = ast.findFirstToken(TokenTypes.OBJBLOCK);
        DetailAST maybeVariableDefinition = classBody.getFirstChild();

        while (maybeVariableDefinition != null) {
            if (maybeVariableDefinition.getType() == TokenTypes.VARIABLE_DEF) {
                DetailAST modifiers = maybeVariableDefinition.findFirstToken(TokenTypes.MODIFIERS);
                if (modifiers != null && isPublic(modifiers) && isStatic(modifiers)) {
                    return true;
                }
            }

            maybeVariableDefinition = maybeVariableDefinition.getNextSibling();
        }

        return false;
    }

    private boolean isPublic(DetailAST modifiers) {
        return modifiers.findFirstToken(TokenTypes.LITERAL_PUBLIC) != null;
    }

    private boolean isStatic(DetailAST modifiers) {
        return modifiers.findFirstToken(TokenTypes.LITERAL_STATIC) != null;
    }
}
