package com.vandenbreemen.grucd.parse

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.StringLiteralExpr

class JavaValueExpressionInteractor() {

    fun toString(expression: Expression): String {
        (expression as? StringLiteralExpr)?.let { strLiteral->
            return strLiteral.value
        }

        (expression as? FieldAccessExpr)?.let { fieldAccessExpr ->
            return fieldAccessExpr.nameAsString;
        }

        return expression.toString();
    }

}