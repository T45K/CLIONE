package io.github.t45k.clione.core

import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
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
class TrackingResultGenerator(
    private val oldCloneSets: InstancedCloneSets,
    private val newCloneSets: InstancedCloneSets,
    private val newIdCloneMap: IdCloneMap
) {
    fun generate(): TrackingResult {
        if (oldCloneSets.isEmpty() && newCloneSets.isEmpty()) {
            return TrackingResult(emptyList(), emptyList(), emptyList(), emptyList())
        }

        val cloneSets: InstancedCloneSets = mapCloneSets()
        val unmergedCloneSets: InstancedCloneSets = cloneSets.filter { cloneSet ->
            val stableClones = cloneSet.count { it.status == CloneStatus.STABLE }
            stableClones > 0 && stableClones + cloneSet.count { it.status == CloneStatus.DELETE } == cloneSet.size
        }

        val inconsistentlyChangedCloneSets = mutableListOf<List<CloneInstance>>()
        val newlyAddedCloneSets = mutableListOf<List<CloneInstance>>()
        val newCloneAddedCloneSets = mutableListOf<List<CloneInstance>>()

        cloneSets.forEach { cloneSet ->
            when {
                cloneSet.isNewlyAddedCloneSets() -> newlyAddedCloneSets.add(cloneSet)
                cloneSet.isNewCloneAddedCloneSets() -> newCloneAddedCloneSets.add(cloneSet)
                else -> inconsistentlyChangedCloneSets.add(cloneSet)
            }
        }

        return TrackingResult(inconsistentlyChangedCloneSets, newlyAddedCloneSets, newCloneAddedCloneSets, unmergedCloneSets)
    }

    private fun mapCloneSets(): InstancedCloneSets =
        newCloneSets.plus(
            oldCloneSets.map { cloneSet ->
                cloneSet.map {
                    if (it.status == CloneStatus.DELETE) {
                        it
                    } else {
                        if (it.mapperCloneInstanceId == -1) {
                            it.mapperCloneInstanceId = it.id
                        }
                        newIdCloneMap[it.mapperCloneInstanceId] ?: error("")
                    }
                }
            })
            .distinctBy { it.toSet() }

    private fun List<CloneInstance>.isNewlyAddedCloneSets(): Boolean =
        this.count { it.status == CloneStatus.ADD } == this.size

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

    fun summarize(): String =
        StringBuilder()
            .appendln("Inconsistently Changed Clone Sets: ${inconsistentlyChangedCloneSets.size}")
            .append(
                inconsistentlyChangedCloneSets.mapIndexed { index, cloneSet ->
                    "clone set $index\n" +
                        cloneSet.joinToString("\n") { "${it.filePath} ${it.startLine}-${it.endLine} ${it.status}" }
                }
            )
            .appendln()
            .appendln("Newly Created Clone Sets: ${newlyCreatedCloneSets.size}")
            .append(
                newlyCreatedCloneSets.mapIndexed { index, cloneSet ->
                    "clone set $index\n" +
                        cloneSet.joinToString("\n") { "${it.filePath} ${it.startLine}-${it.endLine} ${it.status}" }
                }
            )
            .appendln("New Clone Added Clone Sets: ${newCloneAddedCloneSets.size}")
            .append(
                newCloneAddedCloneSets.mapIndexed { index, cloneSet ->
                    "clone set $index\n" +
                        cloneSet.joinToString("\n") { "${it.filePath} ${it.startLine}-${it.endLine} ${it.status}" }
                }
            )
            .appendln("Unmerged Clone Sets: ${unmergedCloneSets.size}")
            .append(
                unmergedCloneSets.mapIndexed { index, cloneSet ->
                    "clone set $index\n" +
                        cloneSet.joinToString("\n") { "${it.filePath} ${it.startLine}-${it.endLine} ${it.status}" }
                }
            )
            .toString()
}
