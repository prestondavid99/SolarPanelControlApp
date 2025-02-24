package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Payload(
    var raise_array: Int = -1
)

/**
{
"isExtended": "true",
"isLifted": "true",
"windSpeed": "72"
}
 */

