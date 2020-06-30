package io.github.t45k.clione.core

import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus

/**
 * Judgement condition:
 * all or all except one clone instances are ADD -> newly added
 * any clone instance is MODIFY -> inconsistently modified
 * else -> new clone added
 */
class SummaryGenerator {
    fun generate(cloneSets: List<List<CloneInstance>>): Summary {
        if (cloneSets.isEmpty()) {
            return Summary(emptyList(), emptyList(), emptyList())
        }

        val inconsistentlyChangedCloneSets = mutableListOf<List<CloneInstance>>()
        val newlyAddedCloneSets = mutableListOf<List<CloneInstance>>()
        val newCloneAddedCloneSets = mutableListOf<List<CloneInstance>>()
        cloneSets.forEach { cloneSet ->
            when {
                cloneSet.count { it.status == CloneStatus.ADD } >= cloneSet.size - 1 -> newlyAddedCloneSets.add(cloneSet)
                cloneSet.any { it.status == CloneStatus.MODIFY } -> inconsistentlyChangedCloneSets.add(cloneSet)
                else -> newCloneAddedCloneSets.add(cloneSet)
            }
        }

        return Summary(inconsistentlyChangedCloneSets, newlyAddedCloneSets, newCloneAddedCloneSets)
    }
}

/**
 * inconsistentlyChangedCloneSets: clone sets in which one or more clone instances are changed, but the others are stable.
 * newlyCreatedCloneSets: clone sets which newly appeared between the pull request.
 * newCloneAddedCloneSets: clone sets created by adding different clone instances to the original clone sets.
 */
data class Summary(
    val inconsistentlyChangedCloneSets: List<List<CloneInstance>>,
    val newlyCreatedCloneSets: List<List<CloneInstance>>,
    val newCloneAddedCloneSets: List<List<CloneInstance>>
) {
    fun isAllEmpty(): Boolean =
        inconsistentlyChangedCloneSets.isEmpty() &&
            newlyCreatedCloneSets.isEmpty() &&
            newCloneAddedCloneSets.isEmpty()
}
