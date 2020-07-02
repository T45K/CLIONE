package io.github.t45k.clione.controller.cloneDetector

import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap

interface CloneDetectorController {

    /**
     * Execute clone detector on new revision.
     * In this execution, not only clone instances but clone candidates in changed files are recorded in IdCloneMap.
     *
     * @param changedFiles files changed in the revision
     * @param initialCloneStatus if this method called in new revision, this value is CloneStatus.ADD.
     *                           On the other hand, called in old revision, this is CloneStatus.DELETE
     *
     * @return CloneSets and IdCloneMap
     */
    fun execute(changedFiles: Set<String>, initialCloneStatus: CloneStatus): Pair<CloneSets, IdCloneMap>
}
