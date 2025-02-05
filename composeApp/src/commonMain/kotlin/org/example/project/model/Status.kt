package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Status(
    var isExtended: Boolean = false,
    var isLifted: Boolean = false,
    var windSpeed: Int = 0
)

/**
{
"isExtended": "true",
"isLifted": "true",
"windSpeed": "72"
}
 */

