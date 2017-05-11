package org.cscenter.hpcourse.prmerger

import org.kohsuke.github.GitHub
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor

data class SourcePaths (
    val repository: String,
    val localtree: String,
    val codepath: String,
    val lab: String
)

@Configuration
@ComponentScan
@EnableScheduling
@PropertySource("classpath:/application.properties")
class MergerConfiguration {
    @Autowired
    lateinit var env: Environment

    @Bean
    fun github(): GitHub = GitHub.connect("username", "token")

    @Bean
    fun sourcePaths() = SourcePaths(
            env.getProperty("repository"),
            env.getProperty("localtree"),
            env.getProperty("codepath"),
            env.getProperty("lab"))

    @Bean
    fun scheduler() = ScheduledThreadPoolExecutor(8)
}
