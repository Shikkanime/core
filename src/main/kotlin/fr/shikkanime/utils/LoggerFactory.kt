package fr.shikkanime.utils

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

class LoggerFactory {
    class LogFormatter : Formatter() {
        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        override fun format(record: LogRecord?): String {
            val message = formatMessage(record)
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            pw.println()
            record?.thrown?.printStackTrace(pw)
            pw.close()
            val throwable: String = sw.toString()
            // %d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
            // Use this format for logback
            return "${this.simpleDateFormat.format(Date())} ${record?.level?.localizedName} ${record?.loggerName} - ${message}${throwable}${if (throwable.isEmpty()) System.lineSeparator() else StringUtils.EMPTY_STRING}"
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