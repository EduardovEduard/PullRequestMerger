package org.cscenter.hpcourse.prmerger

import org.springframework.context.annotation.AnnotationConfigApplicationContext

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val context = AnnotationConfigApplicationContext(MergerConfiguration::class.java)
        context.start()
    }
}
