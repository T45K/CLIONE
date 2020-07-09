package io.github.t45k.clione.controller.cloneDetector.cloneCandidate.java

import org.eclipse.jdt.core.dom.Block
import org.eclipse.jdt.core.dom.CompilationUnit

class JavaBlockExtractor : JDTCloneCandidateExtractor(
    { BlockExtractVisitor(it) }
) {

    private class BlockExtractVisitor(compilationUnit: CompilationUnit) : CloneCandidateExtractVisitor(compilationUnit) {

        override fun visit(node: Block?): Boolean {
            if (node!!.statements().isEmpty() || !node.isMoreThanThreeLines()) {
                return false
            }
            list.add(node)
            return super.visit(node)
        }
    }
}
