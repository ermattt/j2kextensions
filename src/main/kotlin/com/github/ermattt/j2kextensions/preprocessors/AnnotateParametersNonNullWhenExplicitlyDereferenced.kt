package com.github.ermattt.j2kextensions.preprocessors

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiImplUtil.setName
import org.jetbrains.kotlin.j2k.J2kPreprocessorExtension
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class AnnotateParametersNonNullWhenExplicitlyDereferenced : J2kPreprocessorExtension {

    override suspend fun processFiles(
            project: Project,
            files: List<PsiJavaFile>,
    ) {
        for (file in files) {
            val method =
                    readAction {
                        file.classes.firstOrNull()?.findDescendantOfType<PsiMethod> {
                            it.name != "main" &&
                                    !it.isConstructor &&
                                    !it.name.startsWith("get") &&
                                    !it.name.startsWith("set")
                        }
                    } ?: continue

            writeAction { setName(checkNotNull(method.nameIdentifier), "prebar") }
        }
    }
}