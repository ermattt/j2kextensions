package com.github.ermattt.j2kextensions.preprocessors


import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.idea.util.application.executeCommand

class AnnotateParametersCheckedForNullTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData"

    fun testFirstThing() {
        doTest("""
             import javax.annotation.Nullable;
             public class Test  {
                 @Override
                 public void doThing(String s) {
                     if (s == null) return;
                     s.length;
                 }
             }
             """, """
             import javax.annotation.Nullable;
             public class Test  {
                 @Override
                 public void doThing(@Nullable String s) {
                     if (s == null) return;
                     s.length;
                 }
             }
             """)
    }

    private fun doTest(before: String, after: String) {
        myFixture.configureByText("Temp.java", before)
        val psiJavaFile = myFixture.file as PsiJavaFile
        val process = { runBlockingCancellable { AnnotateParametersCheckedForNull().processFiles(myFixture.project, listOf(psiJavaFile)) }}
        myFixture.project.executeCommand("Testing J2K Preprocessors") {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(process, "Testing", true, myFixture.project)
        }
        myFixture.checkResult(after)
    }
}