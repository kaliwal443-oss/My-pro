package com.example.indiangridnavigation

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

class NavigationUtils {
    
    companion object {
        /**
         * Calculate route between two grid points using A* algorithm
         */
        fun calculateGridRoute(startGrid: IndianGridSystem.GridCoordinate, 
                              endGrid: IndianGridSystem.GridCoordinate): List<IndianGridSystem.GridCoordinate> {
            
            if (startGrid == endGrid) {
                return listOf(startGrid)
            }
            
            val openSet = mutableSetOf<IndianGridSystem.GridCoordinate>()
            val closedSet = mutableSetOf<IndianGridSystem.GridCoordinate>()
            val cameFrom = mutableMapOf<IndianGridSystem.GridCoordinate, IndianGridSystem.GridCoordinate>()
            val gScore = mutableMapOf<IndianGridSystem.GridCoordinate, Double>()
            val fScore = mutableMapOf<IndianGridSystem.GridCoordinate, Double>()
            
            openSet.add(startGrid)
            gScore[startGrid] = 0.0
            fScore[startGrid] = heuristicCostEstimate(startGrid, endGrid)
            
            while (openSet.isNotEmpty()) {
                val current = openSet.minByOrNull { fScore.getOrDefault(it, Double.MAX_VALUE) }!!
                
                if (current == endGrid) {
                    return reconstructPath(cameFrom, current)
                }
                
                openSet.remove(current)
                closedSet.add(current)
                
                for (neighbor in IndianGridSystem.getAdjacentGrids(current)) {
                    if (neighbor in closedSet) continue
                    
                    if (!IndianGridSystem.isValidGrid(neighbor.x, neighbor.y)) continue
                    
                    val tentativeGScore = gScore.getOrDefault(current, Double.MAX_VALUE) + 
                                        IndianGridSystem.calculateGridDistance(current, neighbor)
                    
                    if (neighbor !in openSet) {
                        openSet.add(neighbor)
                    } else if (tentativeGScore >= gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        continue
                    }
                    
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + heuristicCostEstimate(neighbor, endGrid)
                }
            }
            
            // Fallback to simple straight line if no path found
            return calculateStraightRoute(startGrid, endGrid)
        }
        
        private fun heuristicCostEstimate(start: IndianGridSystem.GridCoordinate, 
                                         end: IndianGridSystem.GridCoordinate): Double {
            // Use Manhattan distance as heuristic
            return (abs(start.x - end.x) + abs(start.y - end.y)).toDouble()
        }
        
        private fun reconstructPath(cameFrom: Map<IndianGridSystem.GridCoordinate, IndianGridSystem.GridCoordinate>,
                                   current: IndianGridSystem.GridCoordinate): List<IndianGridSystem.GridCoordinate> {
            val path = mutableListOf(current)
            var currentStep = current
            
            while (cameFrom.containsKey(currentStep)) {
                currentStep = cameFrom[currentStep]!!
                path.add(0, currentStep)
            }
            
            return path
        }
        
        private fun calculateStraightRoute(startGrid: IndianGridSystem.GridCoordinate,
                                          endGrid: IndianGridSystem.GridCoordinate): List<IndianGridSystem.GridCoordinate> {
            val route = mutableListOf<IndianGridSystem.GridCoordinate>()
            
            var currentX = startGrid.x
            var currentY = startGrid.y
            
            while (currentX != endGrid.x || currentY != endGrid.y) {
                route.add(IndianGridSystem.GridCoordinate(currentX, currentY, 
                    IndianGridSystem.getGridCode(currentX, currentY)))
                
                when {
                    currentX < endGrid.x -> currentX++
                    currentX > endGrid.x -> currentX--
                    currentY < endGrid.y -> currentY++
                    currentY > endGrid.y -> currentY--
                }
            }
            
            route.add(endGrid)
            return route
        }
        
        /**
         * Calculate bearing between two points
         */
        fun calculateBearing(from: LatLng, to: LatLng): Double {
            val lat1 = Math.toRadians(from.latitude)
            val lat2 = Math.toRadians(to.latitude)
            val lng1 = Math.toRadians(from.longitude)
            val lng2 = Math.toRadians(to.longitude)
            
            val y = sin(lng2 - lng1) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lng2 - lng1)
            
            var bearing = Math.toDegrees(atan2(y, x))
            bearing = (bearing + 360) % 360
            
            return bearing
        }
        
        /**
         * Calculate ETA based on distance and speed
         */
        fun calculateETA(distanceMeters: Double, speedKmph: Double): String {
            if (speedKmph <= 0) return "Unknown"
            
            val timeHours = distanceMeters / 1000 / speedKmph
            val totalMinutes = (timeHours * 60).toInt()
            
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "<1m"
            }
        }
        
        /**
         * Convert meters to kilometers or miles
         */
        fun formatDistance(meters: Double, useMetric: Boolean = true): String {
            return if (useMetric) {
                if (meters >= 1000) {
                    "${String.format("%.1f", meters / 1000)} km"
                } else {
                    "${meters.toInt()} m"
                }
            } else {
                val miles = meters * 0.000621371
                val feet = meters * 3.28084
                
                if (miles >= 0.1) {
                    "${String.format("%.1f", miles)} mi"
                } else {
                    "${feet.toInt()} ft"
                }
            }
        }
    }
}
