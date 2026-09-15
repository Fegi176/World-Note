package app.nodenote.core

/** Bound nesting before the serializer sees untrusted input; braces inside strings are prose. */
fun boundedJson(text: String): String {
    require(text.length <= 64 * 1024 * 1024) { "JSON exceeds 64 MiB text limit" }
    var depth = 0
    var quoted = false
    var escaped = false
    var structures = 0
    text.forEach { char ->
        if (quoted) {
            if (escaped) escaped = false
            else if (char == '\\') escaped = true else if (char == '"') quoted = false
        } else
            when (char) {
                '"' -> quoted = true
                '[',
                '{' -> {
                    depth++
                    structures++
                    require(depth <= 128 && structures <= 1_000_000) {
                        "JSON nesting/resource limit exceeded"
                    }
                }
                ']',
                '}' -> {
                    depth--
                    require(depth >= 0) { "Unbalanced JSON" }
                }
            }
    }
    require(depth == 0 && !quoted) { "Truncated JSON" }
    return text
}
