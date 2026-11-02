package com.example.model

/**
 * Sort mode for playlist and folder containers.
 * CUSTOM order is the default once the user reorders a list.
 */
enum class ContainerSortMode(val displayName: String) {
    CUSTOM("Custom Order ≡"),
    TITLE("Title (A-Z)"),
    ARTIST("Artist (A-Z)"),
    DATE("Date Added"),
    DURATION("Duration"),
    DEFAULT("Default")
}
