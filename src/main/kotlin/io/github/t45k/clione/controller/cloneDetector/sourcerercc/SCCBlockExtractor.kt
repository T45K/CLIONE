package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.entity.CloneStatus
import java.nio.file.Path

// TODO implementation of other languages
interface SCCBlockExtractor {

    /**
     * Extract code blocks(= clone candidates) from target source code.
     *
     * @param code target source code
     * @param filePath target path
     * @param cloneStatus if target file is not modified between the pull request, this value will be STABLE.
     *
     * @return clone candidates, which is CloneInstance and raw string
     */
    fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>>
}
