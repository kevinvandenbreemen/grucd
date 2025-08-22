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
				currentType = Type(newClass, extractPackageName(), TypeType.Class).also { nuType->
					types.add(nuType)
				}

				logger.debug("Added class type: $newClass")
			}
		}

		override fun enterClass_member(ctx: Swift5Parser.Class_memberContext?) {
			super.enterClass_member(ctx)

			logger.debug("Entering class member: ${ctx?.text}")

			NDC.push(ctx?.text ?: "Unknown")

			//  Handle only the top-level member text; do not scan entire class body
			handleMemberText(ctx?.text)
		}

		override fun exitClass_member(ctx: Swift5Parser.Class_memberContext?) {
			super.exitClass_member(ctx)
			NDC.pop()
		}

		// --- Struct-specific handling ---
		override fun enterStruct_declaration(ctx: Swift5Parser.Struct_declarationContext?) {
			super.enterStruct_declaration(ctx)
			val structName = ctx?.struct_name()?.text
			logger.debug("Entering struct declaration: $structName")
			structName?.let { name->
				currentType = Type(name, extractPackageName(), TypeType.Struct).also { nuType->
					types.add(nuType)
				}
			}
		}

		override fun enterStruct_member(ctx: Swift5Parser.Struct_memberContext?) {
			super.enterStruct_member(ctx)
			logger.debug("Entering struct member: ${ctx?.text}")
			NDC.push(ctx?.text ?: "Unknown")
			handleMemberText(ctx?.text)
		}

		override fun exitStruct_member(ctx: Swift5Parser.Struct_memberContext?) {
			super.exitStruct_member(ctx)
			NDC.pop()
		}

		// --- Protocol-specific handling ---
		override fun enterProtocol_declaration(ctx: Swift5Parser.Protocol_declarationContext?) {
			super.enterProtocol_declaration(ctx)
			val protocolName = ctx?.protocol_name()?.text
			logger.debug("Entering protocol declaration: $protocolName")
			protocolName?.let { name->
				currentType = Type(name, extractPackageName(), TypeType.Interface).also { nuType->
					types.add(nuType)
				}
			}
		}

		override fun enterProtocol_member_declaration(ctx: Swift5Parser.Protocol_member_declarationContext?) {
			super.enterProtocol_member_declaration(ctx)
			logger.debug("Entering protocol member: ${ctx?.text}")
			NDC.push(ctx?.text ?: "Unknown")
			handleMemberText(ctx?.text)
		}

		override fun exitProtocol_member_declaration(ctx: Swift5Parser.Protocol_member_declarationContext?) {
			super.exitProtocol_member_declaration(ctx)
			NDC.pop()
		}

		// Remove generic enterEveryRule scanning to avoid collecting local variables as fields

		private fun extractTypeNameFromText(text: String, keyword: String): String? {
			val pattern = Regex("$keyword\\s*(\\w+)")
			val matchResult = pattern.find(text)
			return matchResult?.groupValues?.get(1)
		}

		private fun extractPackageName(): String {
			// For Swift, we'll use the file path as a simple package representation
			return java.io.File(filePath).parent ?: ""
		}

		private fun handleMemberText(memberText: String?) {
			memberText ?: return
			logger.debug("Handling member: $memberText")

			val compact = memberText.replace("\\s+".toRegex(), "")

			// Ignore typealias declarations
			if (compact.startsWith("typealias")) {
				logger.debug("Ignoring typealias member:  raw=$compact")
				return
			}

			// If this is a function member, record method and stop; do not parse inside function body
			if (compact.startsWith("func")) {
				val afterFunc = compact.substringAfter("func")
				val methodName = afterFunc.takeWhile { it.isLetterOrDigit() || it == '_' }
				if (methodName.isNotBlank()) {
					currentType?.addMethod(Method(methodName, "Void"))
					logger.debug("Extracted method: $methodName for type: ${currentType?.name}")
				}
				return
			}

			// Handle properties declared with var/let at member level
			val varIndex = compact.indexOf("var")
			val letIndex = compact.indexOf("let")
			val keywordIndex = listOf(varIndex, letIndex).filter { it >= 0 }.minOrNull() ?: -1
			if (keywordIndex >= 0) {
				val afterKeyword = compact.substring(keywordIndex + 3)
				// Extract field name: first identifier after var/let
				val name = afterKeyword.takeWhile { it.isLetterOrDigit() || it == '_' }
				if (name.isBlank()) return

				val visibility = extractVisibilityFromText(memberText)

				var typeName: String? = null
				// Case 1: explicit type annotation, e.g., var x: Type
				if (afterKeyword.contains(":")) {
					val afterColon = afterKeyword.substringAfter(":")
					typeName = afterColon.takeWhile { it != '=' && it != '{' && it != ';' }.trimEnd()
				}

				// Case 2: inferred from initializer, e.g., var x=TypeName(...)
				if (typeName.isNullOrBlank() && afterKeyword.contains("=")) {
					val rhs = afterKeyword.substringAfter("=")
					val ctorType = rhs.takeWhile { it.isLetterOrDigit() || it == '_' || it == '.' }
					if (ctorType.isNotBlank()) {
						typeName = ctorType.substringAfterLast('.')
					}
				}

				if (typeName.isNullOrBlank()) {
					typeName = "Swift"
				}

				currentType?.addField(Field(name, typeName, visibility))
				logger.debug("Extracted property: $name : $typeName for type: ${currentType?.name}")
			}
		}

		private fun extractMembersFromText(text: String, type: Type) {
			// Deprecated: avoid scanning entire declaration to prevent collecting locals
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