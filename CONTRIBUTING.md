# Contributing to point-my-auth

Thank you for your interest in contributing! This guide will help you get started.

## Development Setup

### Prerequisites

- JDK 21 (LTS) installed at `/usr/local/jdk-21`
- JDK 25 (EA) installed at `/usr/local/jdk-25`
- Maven 3.8+ installed

### Getting Started

```bash
# Clone the repository
git clone https://github.com/pointedsec/point-my-auth.git
cd point-my-auth

# Build and test
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests
```

### JDK Setup

```bash
# JDK 21 (default)
export JAVA_HOME=/usr/local/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# JDK 25 (for testing)
export JAVA_HOME=/usr/local/jdk-25
export PATH=$JAVA_HOME/bin:$PATH
```

## Branch Strategy

- `main` — stable releases
- `develop` — active development (default)
- Feature branches from `develop`
- Tags for releases: `v0.1.0`, `v0.2.0`, etc.

### Creating a Branch

```bash
git checkout develop
git pull origin develop
git checkout -b feat/my-feature
```

## Code Style

### Formatting

We use [Spotless](https://github.com/diffplug/spotless) with Palantir Java Format:

```bash
# Check formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply
```

### Conventions

- **Javadoc** on all public API classes and methods (English)
- **Nullability** annotations from `jakarta.annotation` (`@Nullable`, `@NonNull`)
- **Tests** with JUnit 5 + Mockito + AssertJ
- **Conventional Commits** for commit messages (see below)

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types

| Type | Description |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation changes |
| `style` | Formatting changes (no code change) |
| `refactor` | Code refactoring (no feature/fix) |
| `test` | Adding or updating tests |
| `chore` | Build, CI, or tooling changes |
| `perf` | Performance improvement |

### Examples

```
feat: add @ConditionalAuthorize with SpEL support
fix: correct repeatable annotation AOP pointcut matching
docs: add README with usage examples
test: add integration tests for cache interference
refactor: extract buildContext from doAuthorize to reduce complexity
```

## Pull Request Process

### 1. Create your branch

```bash
git checkout -b feat/my-feature develop
```

### 2. Make your changes

- Write code following the project conventions
- Add tests for new functionality
- Ensure all tests pass: `mvn clean verify`
- Format your code: `mvn spotless:apply`

### 3. Commit

```bash
git add -A
git commit -m "feat: add my feature"
```

### 4. Push and create a PR

```bash
git push -u origin feat/my-feature
```

Then create a Pull Request on GitHub targeting `develop`.

### 5. PR Requirements

- [ ] All CI checks pass (build, tests, Spotless, SonarCloud)
- [ ] New code has tests (aim for >85% coverage)
- [ ] Javadoc on public API
- [ ] No breaking changes (or documented in CHANGELOG)

## Testing

### Running Tests

```bash
# All tests
mvn test

# Specific module
mvn test -pl point-my-auth-core

# Specific test class
mvn test -pl point-my-auth-core -Dtest=AuthorizeEntityAspectIntegrationTest

# With coverage
mvn clean verify -P coverage
```

### Writing Tests

- Use `@SpringBootTest` for integration tests
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Use `assertThat()` from AssertJ for assertions
- Use `assertThatThrownBy()` for exception testing

### Test Utilities

The `point-my-auth-test` module provides:

```java
@PointMyAuthTest
class MyTest {
    @Test
    void test(MockAuthorizationHandler<User> handler) {
        handler.denyAll("denied");
        handler.authorize(context);
        assertThat(handler.wasInvoked()).isTrue();
    }
}
```

## Project Structure

```
point-my-auth-core/           # Core library (annotations, aspect, handler)
point-my-auth-test/           # Test utilities for consumers
point-my-auth-spring-boot-starter/  # Spring Boot starter
examples/point-my-auth-example/     # Example application
```

## Reporting Issues

- Use [GitHub Issues](https://github.com/pointedsec/point-my-auth/issues)
- Include steps to reproduce
- Include expected vs actual behavior
- Include Java version and Spring Boot version

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
