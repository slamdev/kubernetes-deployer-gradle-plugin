package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic
import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.internal.DefaultExecAction
import org.gradle.process.internal.ExecException

import java.nio.file.Files
import java.nio.file.Path

import static java.nio.charset.StandardCharsets.UTF_8
import static java.nio.file.StandardOpenOption.APPEND
import static java.nio.file.StandardOpenOption.CREATE

@CompileStatic
class CommandLineExecutor {

    Project project

    void exec(Path logTo, String prefix, String command, boolean failOnError = true) {
        ExecResult result = project.exec { ExecSpec spec ->
            prepareSpec(spec, command)
            spec.standardOutput = createOutputStream(spec, logTo, prefix)
            project.logger.lifecycle('{}executing: {}', prefix, command)
        }
        if (failOnError && result.exitValue != 0) {
            throw new ExecException("Process '${command}' finished " +
                    "with non-zero exit value ${result.exitValue}")
        }
    }

    private static prepareSpec(ExecSpec spec, String command) {
        boolean windows = System.getProperty('os.name').toLowerCase().contains('windows')
        DefaultExecAction execAction = spec as DefaultExecAction
        execAction.executable(windows ? 'cmd' : 'sh')
                .args(windows ? '/c' : '-c')
                .args(command)
                .redirectErrorStream()
                .setIgnoreExitValue(true)
    }

    private static OutputStream createOutputStream(ExecSpec spec, Path logTo, String prefix) {
        OutputStream prefixedOutput = new PrefixOutputStream(spec.standardOutput, prefix)
        OutputStream fileOutput = Files.newOutputStream(logTo.resolve('exec.log'), APPEND, CREATE)
        new TeeOutputStream(fileOutput, prefixedOutput)
    }

    private static class PrefixOutputStream extends FilterOutputStream {
        String prefix

        PrefixOutputStream(OutputStream os, String prefix) {
            super(os)
            this.prefix = prefix
        }

        void write(byte[] bts, int st, int end) {
            String original = new String(bts, UTF_8)
            bts = (prefix + original).getBytes(UTF_8)
            super.write(bts, st, end + prefix.length())
        }
    }
}
