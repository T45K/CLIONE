package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.core.tokenizer.JDTTokenizer
import io.github.t45k.clione.entity.CloneStatus
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.Block
import org.eclipse.jdt.core.dom.CompilationUnit
import java.nio.file.Path

class JavaSCCBlockExtractor : SCCBlockExtractor {

    /**
     * Extract clone candidates by using JDT AST.
     */
    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> =
        ASTParser.newParser(AST.JLS14)
            .apply { this.setSource(code.toCharArray()) }
            .run { this.createAST(NullProgressMonitor()) as CompilationUnit }
            .let { compilationUnit: CompilationUnit ->
                BlockExtractVisitor(compilationUnit)
                    .apply { compilationUnit.accept(this) }
                    .blocks
                    .map {
                        LazyCloneInstance(filePath.toString(), compilationUnit.getLineNumber(it.startPosition),
                            compilationUnit.getLineNumber(it.startPosition + it.length - 1), cloneStatus,
                            JDTTokenizer().tokenize(it.toString())) to it.toString()
                    }
            }

    private class BlockExtractVisitor(private val compilationUnit: CompilationUnit) : ASTVisitor() {
        val blocks: MutableList<Block> = mutableListOf()

        override fun visit(node: Block?): Boolean {
            if (node!!.statements().isEmpty() || !node.isMoreThanThreeLines()) {
                return false
            }
            blocks.add(node)
            return super.visit(node)
        }

        private fun Block.isMoreThanThreeLines() =
            compilationUnit.getLineNumber(this.startPosition + this.length - 1) -
                compilationUnit.getLineNumber(this.startPosition) > 3
    }
}
