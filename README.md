# QLESS-Bot ☕

A Telegram-based coffee delivery platform designed for school environments. QLESS operates on a mutual assistance system where students can both order drinks and deliver them to earn "QCOINs" - the platform's internal currency.

## 🌟 Features

### For Customers
- **Easy Ordering**: Order coffee, tea, and other beverages through Telegram bot
- **QCOIN System**: Earn QCOINs by delivering drinks, spend them to order your own
- **Real-time Tracking**: Track your order status from placement to delivery
- **Flexible Menu**: Wide selection of hot/cold beverages with customizable syrups
- **Classroom Delivery**: Specify your classroom for convenient delivery

### For Couriers (Q-ers)
- **Earn QCOINs**: Get 1 QCOIN per delivery (2 for first delivery)
- **Flexible Schedule**: Set availability windows or use on-demand delivery
- **Direct Communication**: Connect directly with customers via Telegram
- **Payment Handling**: Manage payments and confirm deliveries

### System Features
- **Time-based Operations**: Configurable operating hours and break times
- **MongoDB Integration**: Persistent data storage for users, orders, and statistics
- **Admin Panel**: Comprehensive statistics and user management
- **Multi-language Support**: Russian interface with emoji-rich UI

## 🏗️ Architecture

### Core Components
- **TelegramBot**: Main bot interface handling user interactions
- **Client**: Customer management with QCOIN balance tracking
- **Courier**: Delivery personnel management with availability scheduling
- **Order**: Order lifecycle management with status tracking
- **Database**: MongoDB integration for data persistence
- **Status System**: Comprehensive order status management

### Technology Stack
- **Language**: Java 11+
- **Framework**: Telegram Bots API (Long Polling)
- **Database**: MongoDB
- **Build Tool**: Gradle with Shadow plugin
- **JSON Processing**: Jackson
- **Deployment**: Heroku (Procfile included)

## 🚀 Getting Started

### Prerequisites
- Java 11 or higher
- MongoDB instance
- Telegram Bot Token
- Gradle

### Environment Variables
```bash
BOT_TOKEN=your_telegram_bot_token
MONGO_CONN_STRING=your_mongodb_connection_string
```

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/QLESS-Bot.git
   cd QLESS-Bot
   ```

2. **Set up environment variables**
   ```bash
   export BOT_TOKEN="your_bot_token"
   export MONGO_CONN_STRING="your_mongodb_connection_string"
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run the application**
   ```bash
   ./gradlew run
   ```

### Deployment

The project includes a `Procfile` for Heroku deployment:

```bash
./gradlew stage
git push heroku main
```

## 📊 Database Schema

### Collections
- **clients**: User profiles with QCOIN balances
- **couriers**: Delivery personnel with schedules
- **orders**: Order history and status tracking
- **admin**: Administrator accounts
- **testers**: Beta tester accounts
- **statistics**: System usage analytics

## 🎯 Usage

### Bot Commands
- `/start` - Initialize bot and get welcome message
- `/order` - Place a new drink order
- `/deliver_now` - Become available for deliveries
- `/balance` - Check your QCOIN balance
- `/cancel` - Cancel current order
- `/support` - Get help and contact information

### Order Flow
1. Customer uses `/order` to start ordering
2. Selects beverage and syrup from menu
3. Specifies delivery classroom
4. System matches with available courier
5. Courier confirms and handles payment
6. Delivery and confirmation process
7. QCOINs transferred upon completion

## ⚙️ Configuration

### Operating Hours
Edit `src/main/resources/AcceptedTime.json` to configure operating hours:
```json
{
  "0": [["09:00", "19:30"]],  // Monday
  "1": [["09:00", "19:30"]],  // Tuesday
  // ... etc
}
```

### Break Times
Edit `src/main/resources/BreaksTime.json` to configure break periods:
```json
{
  "0": [["10:10", "10:15"], ["10:55", "11:05"]],  // Monday breaks
  // ... etc
}
```

## 🔧 Development

### Project Structure
```
src/main/java/org/qless/
├── Main.java              # Application entry point
├── TelegramBot.java       # Bot interface and logic
├── Client.java           # Customer management
├── Courier.java          # Delivery personnel
├── Order.java            # Order management
├── Database.java         # MongoDB operations
├── Status.java           # Order status enum
└── ChatBot.java          # UI messages and menus
```

### Key Features to Extend
- **Payment Integration**: Add support for digital payments
- **Analytics Dashboard**: Web-based admin interface
- **Multi-school Support**: Scale to multiple institutions
- **Mobile App**: Native mobile application
- **API Endpoints**: REST API for external integrations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Telegram Bots API** for the bot framework
- **MongoDB** for data persistence
- **Jackson** for JSON processing
- **Gradle** for build automation

---
**Project Updates**: Follow https://t.me/Q_less

**QLESS** - Making school coffee delivery simple and fun! ☕✨
