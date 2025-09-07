# Contributing to JavaSSG

Thank you for your interest in contributing to JavaSSG! This document provides guidelines and information for contributors.

## Code of Conduct

This project follows a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.9 or higher
- Git

### Development Setup

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/yourusername/JavaSSG.git
   cd JavaSSG
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/originalowner/JavaSSG.git
   ```
4. Install dependencies:
   ```bash
   mvn clean install
   ```

## Development Guidelines

### Code Style

- Follow Java naming conventions
- Use 4-space indentation
- Keep methods small and focused
- Write self-documenting code with meaningful variable and method names
- Use SLF4J for logging instead of System.out in production code

### Testing

- Write unit tests for new functionality
- Use JUnit 5, Mockito, and AssertJ for testing
- Aim for reasonable test coverage
- Run tests before submitting:
  ```bash
  mvn test
  ```

### Documentation

- Update documentation for any new features
- Include JavaDoc comments for public APIs
- Update README.md if installation or usage instructions change
- Add examples for new features

## Submitting Changes

### Pull Request Process

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and commit them:
   ```bash
   git commit -m "Add: brief description of changes"
   ```

3. Push your branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

4. Create a Pull Request on GitHub

### Commit Message Format

Use imperative mood and be concise:

- `Add: feature description`
- `Fix: bug description`
- `Update: component description`
- `Remove: deprecated feature`
- `Refactor: code improvement`

### Pull Request Guidelines

- Provide a clear description of changes
- Link any related issues
- Include screenshots for UI changes
- Ensure all tests pass
- Update documentation as needed

## Issue Reporting

### Bug Reports

When reporting bugs, please include:

- Java version
- Operating system
- Steps to reproduce
- Expected vs actual behavior
- Error messages or logs

### Feature Requests

For feature requests, please provide:

- Clear description of the feature
- Use case and motivation
- Potential implementation approach (if you have ideas)

## Development Areas

### High Priority

- Performance optimizations
- Security enhancements
- Plugin system improvements
- Documentation improvements

### Medium Priority

- Additional template themes
- Enhanced CLI features
- Better error handling
- Internationalization support

### Low Priority

- Additional plugins
- Advanced caching strategies
- Performance monitoring

## Architecture Overview

### Core Components

- `com.javassg.JavaSSG` - Main entry point
- `com.javassg.cli.*` - Command-line interface
- `com.javassg.build.*` - Build pipeline
- `com.javassg.server.*` - Development server
- `com.javassg.parser.*` - Markdown parsing
- `com.javassg.template.*` - Template engine
- `com.javassg.plugin.*` - Plugin system
- `com.javassg.cache.*` - Caching system
- `com.javassg.security.*` - Security features

### Key Design Principles

- Type safety throughout the codebase
- Comprehensive input validation
- Memory-efficient processing
- Extensible plugin architecture
- Security-first approach

## Testing Guidelines

### Unit Tests

- Place tests in `src/test/java/` mirroring the main package structure
- Name test classes with `*Test.java` suffix
- Use descriptive test method names
- Test both positive and negative cases

### Integration Tests

- Test complete workflows
- Verify file system interactions
- Test plugin integration
- Validate configuration loading

### Performance Tests

- Benchmark critical paths
- Test memory usage
- Verify caching effectiveness
- Measure build times

## Security Considerations

When contributing, please consider:

- Input validation and sanitization
- Path traversal prevention
- XSS attack prevention
- Safe file handling
- Resource limits

## Getting Help

- Check existing issues and discussions
- Join our community discussions
- Contact maintainers for guidance

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project documentation

Thank you for contributing to JavaSSG!