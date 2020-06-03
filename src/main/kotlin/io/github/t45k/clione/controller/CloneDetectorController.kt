package io.github.t45k.clione.controller

import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.IdCloneMap

interface CloneDetectorController {
    fun executeOnNewRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap>
    fun executeOnOldRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap>
}
