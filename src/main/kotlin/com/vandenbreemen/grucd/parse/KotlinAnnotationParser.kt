package com.vandenbreemen.grucd.parse

import com.vandenbreemen.grucd.model.Annotation
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.ast.DefaultAstNode
import kotlinx.ast.common.ast.DefaultAstTerminal
import kotlinx.ast.common.klass.KlassAnnotation
import kotlinx.ast.common.klass.KlassIdentifier
import kotlinx.ast.common.klass.KlassString
import kotlinx.ast.common.klass.StringComponentRaw

class KotlinAnnotationParser {

    fun buildAnnotationArguments(
        klassAnnotation: KlassAnnotation,
        annotation: Annotation
    ) {
        klassAnnotation.arguments.forEach { argument ->

            if(argument.keyword == "argument") {
                argument.identifier?.identifier?.let { argName ->
                    var argVal = ""
                    if (argument.children.isNotEmpty()) {
                        argument.children.forEach { argChild->
                            argVal += processArgumentValue(argChild)
                        }

                    }
                    annotation.addArgument(argName, argVal)
                }
            }


        }
    }

    /**
     * Handles the argument value, returning the most appropriate string representation of it
     */
    private fun processArgumentValue(ast: Ast): String {
        when(ast) {
            is KlassString -> {
                var stringRep = ""
                ast.children.forEach { ksChild->
                    stringRep += processArgumentValue(ksChild)
                }

                return stringRep
            }
            is KlassIdentifier, is DefaultAstNode -> {

                if(ast.description == "memberAccessOperator") { //  Ignore these as we need the simpleIdentifier
                    return ""
                }

                //  Don't worry about determining the name of the type of value it is and focus instead on getting a string representation of that value
                var stringRep = ""

                val ksChildren = (ast as? KlassIdentifier)?.children ?: (ast as? DefaultAstNode)?.children
                ksChildren!!.forEach { ksChild->
                    stringRep += processArgumentValue(ksChild)
                }

                return stringRep
            }
            is StringComponentRaw -> {
                return ast.string
            }
            is DefaultAstTerminal -> {
                return ast.text
            }
            else -> {
                return ast.description
            }
        }
    }

}