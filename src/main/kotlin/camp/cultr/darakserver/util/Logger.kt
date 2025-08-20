package camp.cultr.darakserver.util

import org.slf4j.LoggerFactory

/**
 * Interface providing a default logger implementation.
 *
 * Any class implementing this interface gains access to an `org.slf4j.Logger` instance
 * configured for the class in which it is defined. This allows for consistent logging
 * across different components by leveraging SLF4J's logging capabilities.
 *
 * Key Features:
 * - Automatically initializes an SLF4J logger for the implementing class.
 * - Simplifies logging setup by removing the need for manual logger initialization.
 */
interface Logger {
    val logger: org.slf4j.Logger
        get() = LoggerFactory.getLogger(this::class.java)
}