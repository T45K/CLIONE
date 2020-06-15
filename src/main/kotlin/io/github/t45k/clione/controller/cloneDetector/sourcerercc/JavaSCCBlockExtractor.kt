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
    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<DelayCloneInstance> =
        ASTParser.newParser(AST.JLS13)
            .apply { this.setSource(code.toCharArray()) }
            .run { this.createAST(NullProgressMonitor()) as CompilationUnit }
            .let { compilationUnit: CompilationUnit ->
                BlockExtractVisitor()
                    .apply { compilationUnit.accept(this) }
                    .blocks
                    .map {
                        DelayCloneInstance(filePath.toString(), compilationUnit.getLineNumber(it.startPosition),
                            compilationUnit.getLineNumber(it.startPosition + it.length - 1), cloneStatus,
                            JDTTokenizer().tokenize(it.toString()))
                    }
            }

    private class BlockExtractVisitor : ASTVisitor() {
        val blocks: MutableList<Block> = mutableListOf()

        override fun visit(node: Block?): Boolean {
            if (node!!.statements().isEmpty()) {
                return false
            }
            blocks.add(node)
            return super.visit(node)
        }
    }
}
