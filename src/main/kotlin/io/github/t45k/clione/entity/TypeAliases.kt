package io.github.t45k.clione.entity

import java.nio.file.Path

typealias CloneSets = List<Set<Int>>
typealias IdCloneMap = Map<Int, CloneCandidate>
typealias PathClonesMap = Map<Path, List<CloneCandidate>>
typealias BagOfToken = Map<String, Int>
typealias InstancedCloneSets = List<List<CloneCandidate>>
