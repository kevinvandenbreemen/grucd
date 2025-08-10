package com.vandenbreemen.grucd.parse.interactor

import com.vandenbreemen.grucd.model.Type
import com.vandenbreemen.grucd.model.TypeType
import com.vandenbreemen.grucd.model.Method
import com.vandenbreemen.grucd.model.Field
import com.vandenbreemen.grucd.model.Visibility
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import org.apache.log4j.Logger
import java.io.File

// Import the generated ANTLR Swift parser classes
import Swift5Lexer
import Swift5Parser
import Swift5ParserBaseListener
import org.apache.log4j.NDC

class SwiftParsingInteractor() {

    companion object {
        private val logger: Logger = Logger.getLogger(SwiftParsingInteractor::class.java)
    }

    /**
     * Parse a Swift source file and extract Type information
     */
    fun parse(filePath: String): List<Type> {
        logger.debug("Parsing Swift file: $filePath")

        return try {
            val input = CharStreams.fromFileName(filePath)
            val lexer = Swift5Lexer(input)
            val tokens = CommonTokenStream(lexer)

            logger.trace("Got tokens:\n${tokens.tokens.joinToString("\n") { it.text }}")

            val parser = Swift5Parser(tokens)

            // Parse the top-level structure
            val tree = parser.top_level()

            // Create a visitor to extract types
            val visitor = SwiftTypeVisitor(filePath)
            ParseTreeWalker.DEFAULT.walk(visitor, tree)

            visitor.getTypes()
        } catch (e: Exception) {
            logger.error("Error parsing Swift file: $filePath", e)
            emptyList()
        }
    }

    /**
     * Listener class to extract Type information from Swift parse tree
     */
    private class SwiftTypeVisitor(private val filePath: String) : Swift5ParserBaseListener() {

        private var currentType: Type? = null

        private val types = mutableListOf<Type>()
        private val logger: Logger = Logger.getLogger(SwiftTypeVisitor::class.java)

        fun getTypes(): List<Type> = types.toList()


        override fun enterClass_declaration(ctx: Swift5Parser.Class_declarationContext?) {
            super.enterClass_declaration(ctx)

            val className = ctx?.class_name()?.text
            logger.debug( "Entering class declaration: $className", Throwable())

            className?.let { newClass->
                currentType = Type(newClass, "", TypeType.Class).also { nuType->
                    types.add(nuType)
                }

                logger.debug("Added class type: $newClass")
            }
        }

        override fun enterClass_member(ctx: Swift5Parser.Class_memberContext?) {
            super.enterClass_member(ctx)

            logger.debug("Entering class member: ${ctx?.text}")

            NDC.push(ctx?.text ?: "Unknown")

            //  Now to try to handle the member
            handleClassMember(ctx)
        }

        fun handleClassMember(ctx: Swift5Parser.Class_memberContext?) {
            ctx?.let { memberCtx ->
                val memberText = memberCtx.text
                logger.debug("Handling class member: $memberText")

                // Extract method or property from the member context
                if (memberText.startsWith("func")) {
                    val methodName = memberText.substringAfter("func").substringBefore("(").trim()
                    currentType?.addMethod(Method(methodName, "Void")) // Default return type
                    logger.debug("Extracted method: $methodName for type: ${currentType?.name}")
                } else if (memberText.startsWith("var") || memberText.startsWith("let")) {
                    val fieldName = memberText.substringAfter("var").substringBefore(":").trim()

                    val typeName = memberText.substringAfter(":").substringBefore("=").trim()
                    logger.debug("Extracted field type: $typeName for field: $fieldName")

                    val visibility = extractVisibilityFromText(memberText)
                    currentType?.addField(Field(fieldName, typeName, visibility))
                    logger.debug("Extracted property: $fieldName for type: ${currentType?.name}")
                }
            }
        }

        override fun exitClass_member(ctx: Swift5Parser.Class_memberContext?) {
            super.exitClass_member(ctx)
            NDC.pop()
        }

        // Generic approach to handle all declarations by looking for specific patterns in text
        override fun enterEveryRule(ctx: ParserRuleContext?) {
            ctx?.let { context ->
                val text = context.text
                val contextName = context.javaClass.simpleName

                // Handle class declarations
                if (contextName.contains("Class_declarationContext") || text.matches(Regex(".*class\\s+\\w+.*"))) {
                    handleClassDeclaration(context)
                }
                // Handle struct declarations
                else if (contextName.contains("Struct_declarationContext") || text.matches(Regex(".*struct\\s+\\w+.*"))) {
                    handleStructDeclaration(context)
                }
                // Handle protocol declarations
                else if (contextName.contains("Protocol_declarationContext") || text.matches(Regex(".*protocol\\s+\\w+.*"))) {
                    handleProtocolDeclaration(context)
                }
                // Handle enum declarations
                else if (contextName.contains("Enum_declarationContext") || text.matches(Regex(".*enum\\s+\\w+.*"))) {
                    handleEnumDeclaration(context)
                }
            }
        }

        private fun handleClassDeclaration(ctx: ParserRuleContext) {
            val className = extractTypeNameFromText(ctx.text, "class")
            if (className != null) {
                logger.debug("Found class: $className")

                val type = Type(className, extractPackageName(), TypeType.Class)
                extractMembersFromText(ctx.text, type)
                types.add(type)
            }
        }

        private fun handleStructDeclaration(ctx: ParserRuleContext) {
            val structName = extractTypeNameFromText(ctx.text, "struct")
            if (structName != null) {
                logger.debug("Found struct: $structName")

                val type = Type(structName, extractPackageName(), TypeType.Class) // Treat struct as class
                extractMembersFromText(ctx.text, type)
                types.add(type)
            }
        }

        private fun handleProtocolDeclaration(ctx: ParserRuleContext) {
            val protocolName = extractTypeNameFromText(ctx.text, "protocol")
            if (protocolName != null) {
                logger.debug("Found protocol: $protocolName")

                val type = Type(protocolName, extractPackageName(), TypeType.Interface)
                extractMembersFromText(ctx.text, type)
                types.add(type)
            }
        }

        private fun handleEnumDeclaration(ctx: ParserRuleContext) {
            val enumName = extractTypeNameFromText(ctx.text, "enum")
            if (enumName != null) {
                logger.debug("Found enum: $enumName")

                val type = Type(enumName, extractPackageName(), TypeType.Enum)
                extractMembersFromText(ctx.text, type)
                types.add(type)
            }
        }

        private fun extractTypeNameFromText(text: String, keyword: String): String? {
            val pattern = Regex("$keyword\\s+(\\w+)")
            val matchResult = pattern.find(text)
            return matchResult?.groupValues?.get(1)
        }

        private fun extractPackageName(): String {
            // For Swift, we'll use the file path as a simple package representation
            return File(filePath).parent ?: ""
        }

        private fun extractMembersFromText(text: String, type: Type) {
            // Extract methods
            val funcPattern = Regex("func\\s+(\\w+)")
            funcPattern.findAll(text).forEach { match ->
                val methodName = match.groupValues[1]
                val method = Method(methodName, "Void") // Default return type
                type.addMethod(method)
                logger.debug("Extracted method: $methodName for type: ${type.name}")
            }

            // Extract properties
            val varPattern = Regex("(?:var|let)\\s+(\\w+)")
            varPattern.findAll(text).forEach { match ->
                val fieldName = match.groupValues[1]
                val visibility = extractVisibilityFromText(text)
                val field = Field(fieldName, "Swift", visibility) // Default type
                type.addField(field)
                logger.debug("Extracted property: $fieldName for type: ${type.name}")
            }
        }

        private fun extractVisibilityFromText(text: String): Visibility {
            return when {
                text.contains("private") -> Visibility.Private
                text.contains("internal") -> Visibility.Private // Map internal to Private
                text.contains("public") -> Visibility.Public
                text.contains("open") -> Visibility.Public
                else -> Visibility.Internal // Swift default is internal, map to Private
            }
        }
    }
}