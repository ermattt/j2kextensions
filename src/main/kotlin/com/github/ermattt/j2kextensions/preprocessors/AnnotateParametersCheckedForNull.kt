package com.github.ermattt.j2kextensions.preprocessors

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.J2kPreprocessorExtension
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class AnnotateParametersCheckedForNull : J2kPreprocessorExtension {

    override suspend fun processFiles(
            project: Project,
            files: List<PsiJavaFile>,
    ) {
        for (file in files) {
            val parameters =
                    readAction {
                        val methods = file.collectDescendantsOfType<PsiMethod> { it.hasParameters() && "null" in it.body?.text.orEmpty() }

                        methods.flatMap { method ->
                            val body = method.body ?: return@flatMap emptyList()

                            method.parameterList.parameters.filter { parameter ->
                                // todo: ignore primitive types
                                if (parameter.isVarArgs || parameter.probablyHasNullabilityAnnotation() || parameter.name !in body.text) return@filter false
                                body.anyDescendantOfType<PsiBinaryExpression> { binaryExpression ->
                                    if (!binaryExpression.comparesToNull(parameter)) return@anyDescendantOfType false
                                    val ancestorIfStatement = binaryExpression.getParentOfType<PsiIfStatement>(strict = true, PsiIfStatement::class.java)
                                            ?: return@anyDescendantOfType false
                                    !ancestorIfStatement.anyDescendantOfType<PsiThrowStatement>()
                                }
                            }
                        }
                    }
            if (parameters.isEmpty()) continue

            // todo: handle adding import, existing star imports
            val existingNullableAnnotationImport = readAction { file.importList?.importStatements?.firstOrNull { it.qualifiedName?.endsWith(".Nullable") == true }?.qualifiedName }
                    ?: continue
            for (parameter in parameters) {
                val annotation = readAction { PsiElementFactory.getInstance(project).createAnnotationFromText("@Nullable", parameter) }
                writeAction { parameter.addBefore(annotation, parameter.firstChild) }
            }
        }
    }

    private fun PsiParameter.probablyHasNullabilityAnnotation(): Boolean =
            this.annotations.any { it.text.contains("null", ignoreCase = true) }

    private fun PsiBinaryExpression.comparesToNull(parameter: PsiParameter): Boolean {
        if (this.operationSign.text != "==" && this.operationSign.text != "!=") return false
        val parameterReference = when {
            this.lOperand.text == "null" -> this.rOperand
            this.rOperand?.text == "null" -> this.lOperand
            else -> return false
        }
        if (parameterReference !is PsiReferenceExpression || parameterReference.text != parameter.name) return false
        return parameterReference.reference?.resolve() == parameter
    }
}