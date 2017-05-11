package org.cscenter.hpcourse.prmerger

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getAs
import mu.KotlinLogging
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.annotation.PostConstruct

@Component
class Patcher(private val paths: SourcePaths) {
    val logger = KotlinLogging.logger {  }

    fun patch(name: String, patch: String) {
        val patchFileName = "$name.patch"
        if (Files.exists(Paths.get(paths.localtree, "patches", patchFileName))) {
            logger.info { "Already processed $name pull request" }
            return
        }

        Files.write(Paths.get(paths.localtree, "patches", patchFileName), patch.toByteArray(), StandardOpenOption.CREATE_NEW)

        val process = ProcessBuilder().directory(File(paths.localtree)).command(listOf(
                "/bin/bash", "-c", "patch -p1 < patches/$patchFileName"
        )).start()

        if (process.waitFor() != 0) {
            logger.error { "Failed to patch $name! Exited with ${process.exitValue()}" }
            logger.error { process.errorStream.readBytes().contentToString() }
        }
    }
}

@Component
class PullRequestService @Autowired constructor(private val github: GitHub,
                                                private val processor: PullRequestProcessor,
                                                private val paths: SourcePaths) {
    lateinit var repository: GHRepository
    val logger = KotlinLogging.logger {  }

    @PostConstruct
    fun getRepository() {
        repository = github.getRepository(paths.repository)
    }

    @Scheduled(fixedRate = 3600000)
    fun checkPullRequests() {
        repository.getPullRequests(GHIssueState.OPEN).forEach {
            logger.info { "${it.url} ${it.user.login}" }

            it.patchUrl.toString().httpGet().responseString { request, response, result ->
                when (result) {
                    is Result.Success -> {
                        val data = result.getAs<String>()

                        if (data != null) {
                            processor.process(it.user.login, data)
                            logger.info { "===============================" }
                        } else {
                            logger.info { "Received empty patch from ${it.user.login}" }
                        }

                    }
                    is Result.Failure -> throw result.getException()
                }
            }
        }
    }
}
