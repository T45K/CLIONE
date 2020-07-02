package io.github.t45k.clione.entity

import java.nio.file.Path

typealias CloneSets = List<Set<Int>>
typealias IdCloneMap = Map<Int, CloneInstance>
typealias PathClonesMap = Map<Path, List<CloneInstance>>
typealias BagOfToken = Map<String, Int>
