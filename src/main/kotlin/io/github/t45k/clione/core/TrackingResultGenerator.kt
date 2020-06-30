package io.github.t45k.clione.core

import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.InstancedCloneSets

/**
 * Judgement condition:
 * newCloneSets
 * all or all except one clone instances are ADD -> newly added
 * any clone instance is MODIFY -> inconsistently modified
 * at least one clone instance is ADD and the others are all MODIFY or STABLE
 *
 * oldCloneSets
 * some clone instances are DEL and others are STABLE -> unmerged
 */
class TrackingResultGenerator {
    fun generate(oldCloneSets: InstancedCloneSets, newCloneSets: InstancedCloneSets): TrackingResult {
        val unmergedCloneSets: InstancedCloneSets = oldCloneSets.filter { cloneSet ->
            val stableClones = cloneSet.count { it.status == CloneStatus.STABLE }
            stableClones > 0 && stableClones + cloneSet.count { it.status == CloneStatus.DELETE } == cloneSet.size
        }
        if (unmergedCloneSets.isEmpty() && newCloneSets.isEmpty()) {
            return TrackingResult(emptyList(), emptyList(), emptyList(), emptyList())
        }

        val inconsistentlyChangedCloneSets = mutableListOf<List<CloneInstance>>()
        val newlyAddedCloneSets = mutableListOf<List<CloneInstance>>()
        val newCloneAddedCloneSets = mutableListOf<List<CloneInstance>>()
        newCloneSets.forEach { cloneSet ->
            when {
                cloneSet.isNewlyAddedCloneSets() -> newlyAddedCloneSets.add(cloneSet)
                cloneSet.isNewCloneAddedCloneSets() -> newCloneAddedCloneSets.add(cloneSet)
                else -> inconsistentlyChangedCloneSets.add(cloneSet)
            }
        }

        return TrackingResult(inconsistentlyChangedCloneSets, newlyAddedCloneSets, newCloneAddedCloneSets, unmergedCloneSets)
    }

    private fun List<CloneInstance>.isNewlyAddedCloneSets(): Boolean =
        this.count { it.status == CloneStatus.ADD } >= this.size

    private fun List<CloneInstance>.isNewCloneAddedCloneSets(): Boolean =
        this.any { it.status == CloneStatus.ADD } &&
            (this.all { it.status == CloneStatus.ADD || it.status == CloneStatus.MODIFY } ||
                this.all { it.status == CloneStatus.ADD || it.status == CloneStatus.STABLE })
}

/**
 * inconsistentlyChangedCloneSets: clone sets in which one or more clone instances are changed, but the others are stable.
 * newlyCreatedCloneSets: clone sets which newly appeared between the pull request.
 * newCloneAddedCloneSets: clone sets created by adding different clone instances to the original clone sets.
 * unmergedCloneSets: clone sets in which some clone instances are merged but others are stable.
 */
data class TrackingResult(
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

    fun isSingle(): Boolean =
        inconsistentlyChangedCloneSets.size +
            newlyCreatedCloneSets.size +
            newCloneAddedCloneSets.size +
            unmergedCloneSets.size == 1

    fun createSummary(): String {
        val builder = StringBuilder("[Summary]\t")

        if (isAllEmpty()) {
            return builder.appendln("No inconsistently changed, newly created, new clone added, or unmerged clone set is detected.")
                .append("Good job!").toString()
        }

        if (inconsistentlyChangedCloneSets.isNotEmpty()) {
            builder.appendln("Inconsistently changed clone sets: ${inconsistentlyChangedCloneSets.size}")
        }
        if (newlyCreatedCloneSets.isNotEmpty()) {
            builder.appendln("Newly created clone sets: ${newlyCreatedCloneSets.size}")
        }
        if (newCloneAddedCloneSets.isNotEmpty()) {
            builder.appendln("New clone added clone sets: ${newCloneAddedCloneSets.size}")
        }
        if (unmergedCloneSets.isNotEmpty()) {
            builder.appendln("Unmerged clone sets: ${unmergedCloneSets.size}")
        }

        return builder.append("Why don't you modify?").toString()
    }
}
