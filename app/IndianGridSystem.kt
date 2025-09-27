package com.example.indiangridnavigation

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

class IndianGridSystem {
    
    companion object {
        // India bounds
        private const val MIN_LAT = 6.0
        private const val MAX_LAT = 38.0
        private const val MIN_LNG = 68.0
        private const val MAX_LNG = 98.0
        
        // Grid size in degrees
        private const val GRID_SIZE_LAT = 0.1 // Approximately 11 km
        private const val GRID_SIZE_LNG = 0.1 // Approximately 11 km
        
        /**
         * Convert LatLng to Indian Grid Coordinates
         */
        fun latLngToGrid(latLng: LatLng): GridCoordinate {
            require(latLng.latitude in MIN_LAT..MAX_LAT && latLng.longitude in MIN_LNG..MAX_LNG) {
                "Coordinates are outside India bounds"
            }
            
            val gridX = ((latLng.longitude - MIN_LNG) / GRID_SIZE_LNG).toInt()
            val gridY = ((MAX_LAT - latLng.latitude) / GRID_SIZE_LAT).toInt()
            
            return GridCoordinate(gridX, gridY, getGridCode(gridX, gridY))
        }
        
        /**
         * Convert Grid Coordinates to LatLng
         */
        fun gridToLatLng(gridX: Int, gridY: Int): LatLng {
            require(gridX >= 0 && gridY >= 0) { "Grid coordinates must be positive" }
            
            val lng = MIN_LNG + (gridX * GRID_SIZE_LNG)
            val lat = MAX_LAT - (gridY * GRID_SIZE_LAT)
            return LatLng(lat, lng)
        }
        
        /**
         * Generate grid code (e.g., A1, B2, etc.)
         */
        private fun getGridCode(x: Int, y: Int): String {
            val letter = 'A' + (y % 26)
            val number = (x % 100) + 1
            return "$letter$number"
        }
        
        /**
         * Get grid bounds for a specific coordinate
         */
        fun getGridBounds(gridX: Int, gridY: Int): List<LatLng> {
            val northwest = gridToLatLng(gridX, gridY)
            val northeast = gridToLatLng(gridX + 1, gridY)
            val southwest = gridToLatLng(gridX, gridY + 1)
            val southeast = gridToLatLng(gridX + 1, gridY + 1)
            
            return listOf(northwest, northeast, southeast, southwest)
        }
        
        /**
         * Get center point of grid
         */
        fun getGridCenter(gridX: Int, gridY: Int): LatLng {
            val bounds = getGridBounds(gridX, gridY)
            val centerLat = (bounds[0].latitude + bounds[2].latitude) / 2
            val centerLng = (bounds[0].longitude + bounds[2].longitude) / 2
            return LatLng(centerLat, centerLng)
        }
        
        /**
         * Calculate distance between two grid points in meters
         */
        fun calculateGridDistance(grid1: GridCoordinate, grid2: GridCoordinate): Double {
            val latLng1 = getGridCenter(grid1.x, grid1.y)
            val latLng2 = getGridCenter(grid2.x, grid2.y)
            
            return SphericalUtil.computeDistanceBetween(latLng1, latLng2)
        }
        
        /**
         * Get adjacent grids for navigation
         */
        fun getAdjacentGrids(grid: GridCoordinate): List<GridCoordinate> {
            return listOf(
                GridCoordinate(grid.x + 1, grid.y, ""),
                GridCoordinate(grid.x - 1, grid.y, ""),
                GridCoordinate(grid.x, grid.y + 1, ""),
                GridCoordinate(grid.x, grid.y - 1, ""),
                GridCoordinate(grid.x + 1, grid.y + 1, ""),
                GridCoordinate(grid.x + 1, grid.y - 1, ""),
                GridCoordinate(grid.x - 1, grid.y + 1, ""),
                GridCoordinate(grid.x - 1, grid.y - 1, "")
            ).filter { it.x >= 0 && it.y >= 0 }
        }
        
        /**
         * Check if a grid coordinate is valid for India
         */
        fun isValidGrid(gridX: Int, gridY: Int): Boolean {
            return try {
                val latLng = gridToLatLng(gridX, gridY)
                latLng.latitude in MIN_LAT..MAX_LAT && latLng.longitude in MIN_LNG..MAX_LNG
            } catch (e: IllegalArgumentException) {
                false
            }
        }
        
        /**
         * Get all grid codes for a specific region
         */
        fun getGridCodesInRegion(minX: Int, maxX: Int, minY: Int, maxY: Int): List<String> {
            val codes = mutableListOf<String>()
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    if (isValidGrid(x, y)) {
                        codes.add(getGridCode(x, y))
                    }
                }
            }
            return codes
        }
    }
    
    data class GridCoordinate(
        val x: Int,
        val y: Int,
        val code: String
    ) {
        override fun toString(): String {
            return "Grid $code ($x, $y)"
        }
    }
}
