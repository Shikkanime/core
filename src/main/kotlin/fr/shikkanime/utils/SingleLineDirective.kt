package fr.shikkanime.utils

import freemarker.core.Environment
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateModel
import java.io.Writer

class SingleLineDirective : TemplateDirectiveModel {
    override fun execute(
        env: Environment,
        params: MutableMap<Any?, Any?>?,
        loopVars: Array<out TemplateModel>?,
        body: TemplateDirectiveBody?
    ) {
        body?.render(SingleLineWriter(env.out))
    }

    private class SingleLineWriter(private val delegate: Writer) : Writer() {
        private var lastWasWhitespace = false

        override fun write(cbuf: CharArray, off: Int, len: Int) {
            var nonWhitespaceStart = -1
            val end = off + len

            for (i in off..<end) {
                val c = cbuf[i]

                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    if (nonWhitespaceStart != -1) {
                        delegate.write(cbuf, nonWhitespaceStart, i - nonWhitespaceStart)
                        nonWhitespaceStart = -1
                    }

                    if (!lastWasWhitespace) {
                        delegate.write(' '.code)
                        lastWasWhitespace = true
                    }
                } else {
                    if (nonWhitespaceStart == -1) {
                        nonWhitespaceStart = i
                    }
                    lastWasWhitespace = false
                }
            }

            if (nonWhitespaceStart != -1) {
                delegate.write(cbuf, nonWhitespaceStart, end - nonWhitespaceStart)
            }
        }

        override fun flush() = delegate.flush()
        override fun close() = delegate.close()
    }
}
