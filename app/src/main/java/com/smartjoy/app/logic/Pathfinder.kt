package com.smartjoy.app.logic

import com.smartjoy.app.model.PathGraph
import com.smartjoy.app.model.PathNode
import java.util.PriorityQueue

object Pathfinder {

    /**
     * Finds the shortest path between two nodes in the graph using Dijkstra's algorithm.
     * Returns a list of PathNodes in order from start to end.
     */
    fun findShortestPath(graph: PathGraph, startNodeId: String, endNodeId: String): List<PathNode> {
        val distances = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
        val previous = mutableMapOf<String, String?>()
        val nodes = PriorityQueue<Pair<String, Double>>(compareBy { it.second })

        distances[startNodeId] = 0.0
        nodes.add(startNodeId to 0.0)

        while (nodes.isNotEmpty()) {
            val polled = nodes.poll() ?: break
            val (currentId, currentDist) = polled

            if (currentId == endNodeId) break
            if (currentDist > distances.getValue(currentId)) continue

            graph.getNeighbors(currentId).forEach { (neighborId, weight) ->
                val newDist = currentDist + weight
                if (newDist < distances.getValue(neighborId)) {
                    distances[neighborId] = newDist
                    previous[neighborId] = currentId
                    nodes.add(neighborId to newDist)
                }
            }
        }

        val path = mutableListOf<PathNode>()
        var curr: String? = endNodeId
        
        // If there's no path, return empty (and start != end)
        if (previous[endNodeId] == null && startNodeId != endNodeId) return emptyList()

        while (curr != null) {
            graph.nodes.find { it.id == curr }?.let { path.add(0, it) }
            curr = if (curr == startNodeId) null else previous[curr]
        }

        return path
    }
}
