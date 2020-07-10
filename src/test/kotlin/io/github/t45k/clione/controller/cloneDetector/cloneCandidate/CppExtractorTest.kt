package io.github.t45k.clione.controller.cloneDetector.cloneCandidate

import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.cpp.CppBlockExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.cpp.CppFunctionExtractor
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.EMPTY_NAME_PATH
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CppExtractorTest {
    companion object {
        // https://github.com/AllAlgorithms/cpp/blob/master/graphs/dijkstra.cpp
        private const val code = """//
// Dijkstra search algorithm implementation in C++
//
// The All ▲lgorithms Project
//
// https://allalgorithms.com/graphs/
// https://github.com/allalgorithms/cpp
//
// Contributed by: Nikunj Taneja
// Github: @underscoreorcus
//
#include <cstdio>
#include <climits>

// Number of vertices in the graph
#define V 9
int minDistance(int dist[], bool sptSet[])
{
   // Initialize min value
   int min = INT_MAX, min_index;

   for (int v = 0; v < V; v++)
     if (sptSet[v] == false && dist[v] <= min)
         min = dist[v], min_index = v;

   return min_index;
}

void printSolution(int dist[], int n)
{
   printf("Vertex   Distance from Source\n");
   for (int i = 0; i < V; i++)
      printf("%d tt %d\n", i, dist[i]);
}

void dijkstra(int graph[V][V], int src)
{
     int dist[V];     // The output array.  dist[i] will hold the shortest
                      // distance from src to i

     bool sptSet[V]; // sptSet[i] will true if vertex i is included in shortest
                     // path tree or shortest distance from src to i is finalized

     // Initialize all distances as INFINITE and stpSet[] as false
     for (int i = 0; i < V; i++)
        dist[i] = INT_MAX, sptSet[i] = false;

     // Distance of source vertex from itself is always 0
     dist[src] = 0;

     // Find shortest path for all vertices
     for (int count = 0; count < V-1; count++)
     {
       // Pick the minimum distance vertex from the set of vertices not
       // yet processed. u is always equal to src in the first iteration.
       int u = minDistance(dist, sptSet);

       // Mark the picked vertex as processed
       sptSet[u] = true;

       // Update dist value of the adjacent vertices of the picked vertex.
       for (int v = 0; v < V; v++)

         // Update dist[v] only if is not in sptSet, there is an edge from
         // u to v, and total weight of path from src to  v through u is
         // smaller than current value of dist[v]
         if (!sptSet[v] && graph[u][v] && dist[u] != INT_MAX
                                       && dist[u]+graph[u][v] < dist[v])
            dist[v] = dist[u] + graph[u][v];
     }
     printSolution(dist, V);
}

int main()
{
   /* Sample graph */
   int graph[V][V] = {{0, 4, 0, 0, 0, 0, 0, 8, 0},
                      {4, 0, 8, 0, 0, 0, 0, 11, 0},
                      {0, 8, 0, 7, 0, 4, 0, 0, 2},
                      {0, 0, 7, 0, 9, 14, 0, 0, 0},
                      {0, 0, 0, 9, 0, 10, 0, 0, 0},
                      {0, 0, 4, 14, 10, 0, 2, 0, 0},
                      {0, 0, 0, 0, 0, 2, 0, 1, 6},
                      {8, 11, 0, 0, 0, 0, 1, 0, 7},
                      {0, 0, 2, 0, 0, 0, 6, 7, 0}
                     };
    dijkstra(graph, 0);
    return 0;
}
"""
    }

    @Test
    fun testBlock() {
        val blocks: List<Pair<LazyCloneInstance, String>> =
            CppBlockExtractor().extract(code, EMPTY_NAME_PATH, CloneStatus.STABLE)
        assertEquals(5, blocks.size)
    }

    @Test
    fun testFunction() {
        val functions: List<Pair<LazyCloneInstance, String>> =
            CppFunctionExtractor().extract(code, EMPTY_NAME_PATH, CloneStatus.STABLE)
        assertEquals(4, functions.size)
    }
}