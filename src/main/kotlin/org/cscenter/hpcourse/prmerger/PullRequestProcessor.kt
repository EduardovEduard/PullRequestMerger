package org.cscenter.hpcourse.prmerger

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

@Component
class PullRequestProcessor @Autowired constructor(private val patcher: Patcher,
                                                  private val paths: SourcePaths,
                                                  private val tester: CodeTester) {
    val logger = KotlinLogging.logger {  }

    @Synchronized fun process(name: String, patch: String) {
        val dirsBefore = getStudentDirectories()

        patcher.patch(name, patch)

        val dirsAfter = getStudentDirectories()
        val newDir = dirsAfter.subtract(dirsBefore).firstOrNull()

        if (newDir == null) {
            logger.error { "No new directory was created during $name patch" }
        } else {
            val pullRequestDir = Paths.get(paths.codepath, paths.lab, newDir)
            logger.info { "Building $pullRequestDir..." }
            val builderChain = BuilderChain(pullRequestDir.toString())
            if (!builderChain.supported()) {
                logger.error { "Invalid pull request! Build not supported" }
                logger.error { Files.list(pullRequestDir).collect(Collectors.toList()).joinToString(" ") }
            } else {
                val executable = newFiles(pullRequestDir) {
                    builderChain.build()
                }.map { Paths.get(pullRequestDir.toString(), it) }
                 .filter { Files.isExecutable(it) && !Files.isDirectory(it) }.firstOrNull()

                if (executable == null) {
                    logger.error { "No executable produced during $name build!" }
                    return
                }

                logger.info { "Built executable $executable for pull request $name" }
                tester.test(executable)
            }
        }
    }

    private fun listFiles(path: Path): List<String> {
        return Files.list(path)
                .map { it.fileName.toString() }
                .collect(Collectors.toList())
    }

    private fun getStudentDirectories() =
        Files.list(Paths.get(paths.codepath, paths.lab))
                .filter { Files.isDirectory(it) }
                .map { it.fileName.toString() }
                .collect(Collectors.toList())

    private fun newFiles(dir: Path, block: () -> Unit): Set<String> {
        val filesBefore = listFiles(dir)
        block()
        val filesAfter = listFiles(dir)
        return filesAfter.subtract(filesBefore)
    }
}