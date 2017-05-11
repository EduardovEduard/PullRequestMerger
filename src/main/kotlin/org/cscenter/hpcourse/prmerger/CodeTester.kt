package org.cscenter.hpcourse.prmerger

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

interface CodeTester {
    fun test(executable: Path)
}

@Component
class PthreadLabTester @Autowired constructor(loader: ResourceLoader): CodeTester {
    val logger = KotlinLogging.logger {  }

    private val testFile = loader.getResource("classpath:test.txt").file

    override fun test(executable: Path) {
        val answer = testFile.readText().trim().split(' ').sumBy(String::toInt)

        val process = ProcessBuilder().command(listOf(
                "$executable"
        ))
        process.redirectInput(testFile).redirectOutput(ProcessBuilder.Redirect.INHERIT)

        val tempFile = File.createTempFile("pthread", "lab")
        try {
            process.redirectOutput(tempFile)
            val startedProcess = process.start()
            val exited = startedProcess.waitFor(5, TimeUnit.SECONDS)
            if (!exited) {
                startedProcess.destroyForcibly()
                logger.error { "$executable failed to finish in 5secs. Possible deadlock!" }
            }

            val exitCode = startedProcess.exitValue()
            if (exitCode != 0) {
                logger.error { "Failed test of $executable" }
            } else {
                val programOutput = tempFile.readText().trim().toInt()
                if (programOutput == answer) {
                    logger.info { "Tested $executable OK!" }
                } else {
                    logger.error { "$executable: $programOutput != $answer" }
                }
            }
        } catch (e: Exception) {
            logger.error { "Exception occured during testing: $e" }
        } finally {
            tempFile.delete()
        }
    }
}