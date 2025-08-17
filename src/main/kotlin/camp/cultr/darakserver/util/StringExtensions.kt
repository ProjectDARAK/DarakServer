package camp.cultr.darakserver.util

/**
 * Normalizes the string by removing leading and trailing whitespace
 * and converting all characters to lowercase.
 *
 * This utility function ensures a consistent format for string values,
 * which can be useful for operations like string comparison or data normalization.
 *
 * @receiver The input string to be normalized.
 * @return A new, normalized string with trimmed whitespace and all characters in lowercase.
 */
fun String.normalize() = this.trim().lowercase()