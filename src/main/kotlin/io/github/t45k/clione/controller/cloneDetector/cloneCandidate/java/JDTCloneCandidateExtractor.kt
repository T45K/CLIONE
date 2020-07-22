package io.github.t45k.clione.controller.cloneDetector.cloneCandidate.java

import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.CloneCandidateExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.LazyCloneInstance
import io.github.t45k.clione.core.tokenizer.JDTTokenizer
import io.github.t45k.clione.entity.CloneStatus
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.CompilationUnit
import java.nio.file.Path

abstract class JDTCloneCandidateExtractor(
    private val instantiateVisitor: (compilationUnit: CompilationUnit) -> CloneCandidateExtractVisitor
) : CloneCandidateExtractor {

    /**
     * Extract clone candidates by using JDT AST.
     */
    override fun extract(
        code: String,
        filePath: Path,
        cloneStatus: CloneStatus
    ): List<Pair<LazyCloneInstance, String>> =
        ASTParser.newParser(AST.JLS14)
            .apply { this.setSource(code.toCharArray()) }
            .run { this.createAST(NullProgressMonitor()) as CompilationUnit }
            .let { compilationUnit: CompilationUnit ->
                instantiateVisitor(compilationUnit)
                    .apply { compilationUnit.accept(this) }
                    .list
                    .map {
                        LazyCloneInstance(
                            filePath, compilationUnit.getLineNumber(it.startPosition),
                            compilationUnit.getLineNumber(it.startPosition + it.length), cloneStatus,
                            JDTTokenizer().tokenize(it.toString())
                        ) to it.toString()
                    }
            }
}

abstract class CloneCandidateExtractVisitor(private val compilationUnit: CompilationUnit) : ASTVisitor() {
    val list: MutableList<ASTNode> = mutableListOf()

    protected fun ASTNode.isMoreThanThreeLines() =
        compilationUnit.getLineNumber(this.startPosition + this.length) -
            compilationUnit.getLineNumber(this.startPosition) + 1 > 3
}
