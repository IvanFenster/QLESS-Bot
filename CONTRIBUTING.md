# Contributing to QLESS-Bot

Thank you for your interest in contributing to QLESS-Bot! This document provides guidelines and information for contributors.

## 🚀 Getting Started

1. **Fork the repository**
2. **Clone your fork**
   ```bash
   git clone https://github.com/yourusername/QLESS-Bot.git
   cd QLESS-Bot
   ```
3. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

## 🔧 Development Setup

### Prerequisites
- Java 11 or higher
- Gradle
- MongoDB instance
- Telegram Bot Token

### Environment Setup
1. Copy the example configuration:
   ```bash
   cp config.example.json src/main/resources/config.json
   ```
2. Edit `src/main/resources/config.json` with your settings
3. Set environment variables:
   ```bash
   export BOT_TOKEN="your_bot_token"
   export MONGO_CONN_STRING="your_mongodb_connection_string"
   ```

### Building and Running
```bash
./gradlew build
./gradlew run
```

## 📝 Code Style Guidelines

- Follow Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise
- Use proper exception handling

## 🧪 Testing

Before submitting a pull request:
1. Ensure the code compiles without errors
2. Test the bot functionality
3. Verify database operations work correctly
4. Check that configuration changes are properly applied

## 📋 Pull Request Process

1. **Update documentation** if needed
2. **Test your changes** thoroughly
3. **Commit with clear messages**
   ```bash
   git commit -m "Add feature: brief description"
   ```
4. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
5. **Create a Pull Request** with:
   - Clear description of changes
   - Any breaking changes noted
   - Screenshots if UI changes

## 🐛 Reporting Issues

When reporting bugs, please include:
- **Environment**: OS, Java version, MongoDB version
- **Steps to reproduce**: Clear step-by-step instructions
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Logs**: Any error messages or logs

## 💡 Feature Requests

For feature requests:
- Describe the feature clearly
- Explain the use case
- Consider implementation complexity
- Suggest potential approaches

## 📞 Getting Help

- **Issues**: Use GitHub Issues
- **Discussions**: Use GitHub Discussions
- **Code Review**: Tag maintainers in PRs

## 🎯 Areas for Contribution

- **Bug fixes**: Check issues labeled "bug"
- **Documentation**: Improve README, add examples
- **Features**: Check issues labeled "enhancement"
- **Testing**: Add unit tests, integration tests
- **Performance**: Optimize database queries, reduce latency

## 📄 License

By contributing to QLESS-Bot, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to QLESS-Bot! 🎉
