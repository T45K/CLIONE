package io.github.t45k.clione.core

import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.InstancedCloneSets

/**
 * Judgement condition:
 * newCloneSets
 * all or all except one clone instances are ADD -> newly added
 * any clone instance is MODIFY -> inconsistently modified
 * else -> new clone added
 *
 * oldCloneSets
 * some clone instances are DEL and others are STABLE -> unmerged
 */
class SummaryGenerator {
    fun generate(oldCloneSets: InstancedCloneSets, newCloneSets: InstancedCloneSets): Summary {
        val unmergedCloneSets: InstancedCloneSets = oldCloneSets.filter { cloneSet ->
            val stableClones = cloneSet.count { it.status == CloneStatus.STABLE }
            stableClones > 0 && stableClones + cloneSet.count { it.status == CloneStatus.DELETE } == cloneSet.size
        }
        if (unmergedCloneSets.isEmpty() && newCloneSets.isEmpty()) {
            return Summary(emptyList(), emptyList(), emptyList(), emptyList())
        }

        val inconsistentlyChangedCloneSets = mutableListOf<List<CloneInstance>>()
        val newlyAddedCloneSets = mutableListOf<List<CloneInstance>>()
        val newCloneAddedCloneSets = mutableListOf<List<CloneInstance>>()
        newCloneSets.forEach { cloneSet ->
            when {
                cloneSet.count { it.status == CloneStatus.ADD } >= cloneSet.size - 1 -> newlyAddedCloneSets.add(cloneSet)
                cloneSet.any { it.status == CloneStatus.MODIFY } -> inconsistentlyChangedCloneSets.add(cloneSet)
                else -> newCloneAddedCloneSets.add(cloneSet)
            }
        }

        return Summary(inconsistentlyChangedCloneSets, newlyAddedCloneSets, newCloneAddedCloneSets, unmergedCloneSets)
    }
}

/**
 * inconsistentlyChangedCloneSets: clone sets in which one or more clone instances are changed, but the others are stable.
 * newlyCreatedCloneSets: clone sets which newly appeared between the pull request.
 * newCloneAddedCloneSets: clone sets created by adding different clone instances to the original clone sets.
 * unmergedCloneSets: clone sets in which some clone instances are merged but others are stable.
 */
data class Summary(
    val inconsistentlyChangedCloneSets: InstancedCloneSets,
    val newlyCreatedCloneSets: InstancedCloneSets,
    val newCloneAddedCloneSets: InstancedCloneSets,
    val unmergedCloneSets: InstancedCloneSets
) {
    fun isAllEmpty(): Boolean =
        inconsistentlyChangedCloneSets.isEmpty() &&
            newlyCreatedCloneSets.isEmpty() &&
            newCloneAddedCloneSets.isEmpty() &&
            unmergedCloneSets.isEmpty()
}
