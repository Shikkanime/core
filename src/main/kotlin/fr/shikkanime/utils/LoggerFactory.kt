package fr.shikkanime.utils

import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.*

class LoggerFactory {
    class LogFormatter : Formatter() {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        override fun format(record: LogRecord?): String {
            val message = formatMessage(record)

            val throwable = record?.thrown?.let {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                pw.println()
                it.printStackTrace(pw)
                pw.close()
                sw.toString()
            } ?: System.lineSeparator()

            // %d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
            // Use this format for logback
            return "${ZonedDateTime.now().format(dateTimeFormatter)} ${record?.level?.localizedName} ${record?.loggerName} - $message$throwable"
        }
    }

    companion object {
        private val map = mutableMapOf<String, Logger>()

        private fun buildLogger(name: String): Logger {
            val logger = Logger.getLogger(name)
            logger.useParentHandlers = false
            val consoleHandler = ConsoleHandler()
            consoleHandler.formatter = LogFormatter()
            consoleHandler.level = Level.ALL
            logger.addHandler(consoleHandler)
            logger.level = Level.ALL
            return logger
        }

        fun getLogger(name: String) = map.getOrPut(name) { buildLogger(name) }
        fun getLogger(clazz: Class<*>) = map.getOrPut(clazz.name) { buildLogger(clazz.name) }
    }
}