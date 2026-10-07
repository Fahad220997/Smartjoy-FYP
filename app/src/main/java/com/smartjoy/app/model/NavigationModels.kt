package com.smartjoy.app.model

import com.smartjoy.app.logic.LocationUtils

enum class AttractionCategory {
    ANIMAL_ENCLOSURE,
    FACILITY,
    ENTRANCE
}

data class Attraction(
    val id: String,
    val name: String,
    val description: String,
    val category: AttractionCategory,
    val location: ZooLocation
)

data class PathNode(
    val id: String,
    val location: ZooLocation,
    val name: String? = null
)

data class PathEdge(
    val fromId: String,
    val toId: String,
    val distanceMeters: Double
)

class PathGraph(
    val nodes: List<PathNode>,
    val edges: List<PathEdge>
) {
    fun getNeighbors(nodeId: String): List<Pair<String, Double>> {
        return edges.filter { it.fromId == nodeId }.map { it.toId to it.distanceMeters } +
               edges.filter { it.toId == nodeId }.map { it.fromId to it.distanceMeters }
    }
}

val sampleAttractions = listOf(
    Attraction(
        id = "main_entrance",
        name = "Main Entrance",
        description = "Peshawar Zoo entrance and ticket counter.",
        category = AttractionCategory.ENTRANCE,
        location = ZooLocation(34.017306, 71.489722)
    ),
    Attraction(
        id = "lion_enclosure",
        name = "Lion Enclosure",
        description = "Home to the park's African lions.",
        category = AttractionCategory.ANIMAL_ENCLOSURE,
        location = ZooLocation(34.017506, 71.490122)
    ),
    Attraction(
        id = "elephant_house",
        name = "Elephant House",
        description = "See the resident elephants up close.",
        category = AttractionCategory.ANIMAL_ENCLOSURE,
        location = ZooLocation(34.017806, 71.489922)
    ),
    Attraction(
        id = "food_court",
        name = "Food Court",
        description = "Snacks, drinks, and seating area.",
        category = AttractionCategory.FACILITY,
        location = ZooLocation(34.017106, 71.490522)
    ),
    Attraction(
        id = "restrooms",
        name = "Restrooms",
        description = "Public restroom facilities.",
        category = AttractionCategory.FACILITY,
        location = ZooLocation(34.017006, 71.489422)
    )
)

/**
 * PLACEHOLDER - Sample graph data for Peshawar Zoo internal footpaths.
 */
val zooPathGraph: PathGraph by lazy {
    val nodes = listOf(
        PathNode("entrance", ZooLocation(34.017306, 71.489722), "Main Entrance"),
        PathNode("j1", ZooLocation(34.017400, 71.489900)),
        PathNode("j2", ZooLocation(34.017200, 71.490200)),
        PathNode("food_court", ZooLocation(34.017106, 71.490522), "Food Court"),
        PathNode("lion_house", ZooLocation(34.017506, 71.490122), "Lion House"),
        PathNode("restroom", ZooLocation(34.017006, 71.489422), "Restrooms")
    )

    val nodeMap = nodes.associateBy { it.id }
    
    fun createEdge(from: String, to: String): PathEdge {
        val dist = LocationUtils.calculateDistance(nodeMap[from]!!.location, nodeMap[to]!!.location).toDouble()
        return PathEdge(from, to, dist)
    }

    val edges = listOf(
        createEdge("entrance", "j1"),
        createEdge("j1", "lion_house"),
        createEdge("j1", "j2"),
        createEdge("j2", "food_court"),
        createEdge("entrance", "restroom")
    )

    PathGraph(nodes, edges)
}
