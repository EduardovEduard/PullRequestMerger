package org.cscenter.hpcourse.prmerger

import com.sun.org.apache.bcel.internal.classfile.Code
import com.sun.org.apache.xpath.internal.operations.Bool
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

interface CodeBuilder {
    fun supported(): Boolean
    fun build(): Boolean
}

fun String.shell(workDir: String): Boolean {
    val process = ProcessBuilder().directory(File(workDir))
            .command("/bin/bash", "-c", this).start()
    return process.waitFor() == 0
}

class MakeBuilder(private val codepath: String): CodeBuilder {
    val path get() = Paths.get(codepath)

    override fun supported(): Boolean {
        return Files.list(path).anyMatch { it.fileName.toString().toLowerCase() == "makefile" }
    }

    override fun build() = "make".shell(codepath)
}

class CMakeBuilder(private val codepath: String): CodeBuilder {
    val path get() = Paths.get(codepath)

    override fun supported(): Boolean {
        return Files.list(path).anyMatch { it.fileName.toString().toLowerCase() == "cmakelists.txt" }
    }

    override fun build() = "cmake .".shell(codepath) && "make".shell(codepath)
}


class GCCBuilder(private val codepath: String): CodeBuilder {
    val path get() = Paths.get(codepath)

    override fun supported(): Boolean {
        val fileList = Files.list(path).collect(Collectors.toList())
        return fileList.any { listOf(".c", ".cpp", ".cxx").any { ext -> it.fileName.toString().endsWith(ext) } }
    }

    override fun build(): Boolean {
        val fileList = Files.list(path).collect(Collectors.toList())
        val cppfiles = fileList.filter { listOf(".c", ".cpp", ".cxx")
                .any { ext -> it.fileName.toString().endsWith(ext) } }
                .map { it.fileName.toString() }

        return "g++ -std=c++11 -pthread ${cppfiles.joinToString(" ")} -o lab".shell(codepath)
    }
}

class BuilderChain(private val codepath: String): CodeBuilder {
    val builders = listOf(CMakeBuilder(codepath), MakeBuilder(codepath), GCCBuilder(codepath))

    override fun supported(): Boolean {
        return builders.any(CodeBuilder::supported)
    }

    override fun build(): Boolean {
        return builders.first(CodeBuilder::supported).build()
    }
}