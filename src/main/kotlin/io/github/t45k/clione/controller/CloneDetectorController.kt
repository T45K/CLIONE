package io.github.t45k.clione.controller

import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.IdCloneMap

interface CloneDetectorController {

    /**
     * Execute clone detector on new revision.
     * In this execution, not only clone instances but clone candidates are recorded in IdCloneMap.
     */
    fun executeOnNewRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap>

    /**
     * Execute clone detector on old revision
     */
    fun executeOnOldRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap>
}
