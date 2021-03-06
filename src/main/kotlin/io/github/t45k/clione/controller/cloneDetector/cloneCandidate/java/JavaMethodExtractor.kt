package io.github.t45k.clione.controller.cloneDetector.cloneCandidate.java

import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.core.dom.MethodDeclaration

class JavaMethodExtractor : JDTCloneCandidateExtractor(
    { MethodExtractVisitor(it) }
) {

    private class MethodExtractVisitor(compilationUnit: CompilationUnit) :
        CloneCandidateExtractVisitor(compilationUnit) {

        override fun visit(node: MethodDeclaration): Boolean {
            if (node.isConstructor || node.body?.isMoreThanThreeLines() == false) {
                return false
            }

            node.javadoc = null
            list.add(node)
            return super.visit(node)
        }
    }
}
