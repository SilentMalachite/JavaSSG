package com.javassg.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CLIコマンドの統合テスト
 */
class CommandLineInterfaceTest {

    private CommandLineInterface cli;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        cli = new CommandLineInterface();
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void shouldDisplayHelpWhenNoArgs() {
        String[] args = {};
        
        int exitCode = cli.run(args);
        
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Usage: javassg");
        assertThat(output).contains("serve");
        assertThat(output).contains("build");
        assertThat(output).contains("new");
    }

    @Test
    void shouldDisplayHelpWithHelpCommand() {
        String[] args = {"--help"};
        
        int exitCode = cli.run(args);
        
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Usage: javassg");
        assertThat(output).contains("Options:");
        assertThat(output).contains("Commands:");
    }

    @Test
    void shouldDisplayVersionWithVersionFlag() {
        String[] args = {"--version"};
        
        int exitCode = cli.run(args);
        
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("JavaSSG");
        assertThat(output).contains("1.0.0");
    }

    @Test
    void shouldHandleInvalidCommand() {
        String[] args = {"invalid-command"};
        
        int exitCode = cli.run(args);
        
        assertThat(exitCode).isEqualTo(1);
        String output = outputStream.toString();
        assertThat(output).contains("Unknown command");
        assertThat(output).contains("invalid-command");
    }

    @Test
    void shouldDisplayGlobalOptions() {
        String[] args = {"--help"};
        
        cli.run(args);
        
        String output = outputStream.toString();
        assertThat(output).contains("--config");
        assertThat(output).contains("--verbose");
        assertThat(output).contains("--quiet");
        assertThat(output).contains("--working-directory");
    }
}