package org.qless;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import static org.qless.Main.*;


public class TelegramBot extends ChatBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private ScheduledExecutorService executionScheduler;
    private int searchingCourierTime = 8;
    private int recievingOrdersNowTime = 15;
    private boolean notificationSend = false;
    private double[][] tokensChances = {
            {0.395, 0},
            {0.55, 1},
            {0.05, 2},
            {0.005, 3}
    };

    public static int num_of_starts = 0;
    public static int num_of_init_orders = 0;
    public static int num_of_launching_orders = 0;
    public static int num_of_matched_orders = 0;
    public static int num_of_completed_orders = 0;
    public static int num_of_deliver_now = 0;
    public static int num_of_turning_off_one_day = 0;
    public static int num_of_turning_off_forever = 0;
    public static int num_of_turning_on = 0;
    public static int num_of_blocking = 0;
    public static int num_of_accept_to_deliver = 0;
    public static int num_of_send_to_deliver = 0;

    public TelegramBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
        this.executionScheduler = Executors.newScheduledThreadPool(2);
        this.num_of_starts = 0;
        this.num_of_init_orders = 0;
        this.num_of_launching_orders = 0;
        this.num_of_matched_orders = 0;
        this.num_of_completed_orders = 0;
        this.num_of_deliver_now = 0;
        this.num_of_turning_off_one_day = 0;
        this.num_of_turning_off_forever = 0;
        this.num_of_turning_on = 0;
        this.num_of_blocking = 0;
        this.num_of_accept_to_deliver = 0;
        this.num_of_send_to_deliver = 0;

        //scheduleNextWindow("src/main/resources/BreaksTime.json");
    }

    public void sendNotification() {
        LocalDateTime time = LocalDateTime.of(2025, 2, 10, 9, 30, 00);
        if (time.isAfter(LocalDateTime.now())) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    for (Client client : allClients) {
                        if ((client.getNumberOfTokens() == 0) & (client.getTgNickname().equalsIgnoreCase("ivanfenster"))) {
                            sendMessage(client.getTgID(), "Привет! Наш сервис начинает свою работу прямо сейчас! \uD83C\uDF89\uD83C\uDF89\n\n\uD83D\uDC8E Текущее кол-во QCOINов у тебя: 0. Чтобы получить 1 QCOIN, доставь кому-то напиток, когда у тебя будет возможноть, командной /deliver_now. Тогда ты получишь возможность заказать кофе к кабинету и себе.\n\n\uD83D\uDCDC Обрати внимание, что клиент переведит Q-erу ровно столько, сколько стоит напиток. Тебе не придется покупать кофе за свои деньги, но очень желательно, чтобы ты мог проверить, перевели ли тебе деньги за напиток");
                        }
                    }
                }
            };

            this.executionScheduler.schedule(task, Duration.between(LocalDateTime.now(), time).getSeconds(), TimeUnit.SECONDS);
        }
    }

    public void calculate_statistic() {
        int num_blocked = 0;
        int num_clients = 0;
        int num_muted = 0;
        int num_tokens_0 = 0;
        int num_tokens_1 = 0;
        int num_client_orders_more_0 = 0;


        for (Client client : allClients) {
            num_clients++;
            if (client.getBlockedTheBot()) {
                num_blocked++;
            }
            if (!client.getNotificationsAllowed()){
                num_muted ++;
            }
            if (client.getNumberOfTokens() == 0) {
                num_tokens_0++;
            }
            if (client.getNumberOfTokens() == 1) {
                num_tokens_1++;
            }
            if (client.getNumberOfOrders() > 0){
                num_client_orders_more_0++;
            }
        }

        int num_couriers = 0;
        int num_scheduled_couriers = 0;
        int num_couriers_orders_more_0 = 0;

        for (Courier courier: allCouriers) {
            num_couriers++;
            if (courier.getSchedule() != null) {
                num_scheduled_couriers ++;
            }
            if (courier.getNumberOfOrders() > 0) {
                num_couriers_orders_more_0++;
            }
        }



    }

    public Courier findCourierByChatID(long ChatID) {
        for (Courier courier : allCouriers) {
            if (courier.getTgID() == ChatID) {
                return courier;
            }
        }
        System.out.println("ATTENTION: findCourierByChatID returned null.");
        return null;
    }

    public Client findClientByNickname(String nickname) {
        for (Client client : allClients) {
            if (client.getTgNickname().equalsIgnoreCase(nickname)) {
                return client;
            }
        }
        return null;
    }

    // Checks if string starts with slash
    public static boolean startsWithSlash(String str) {
        if (str == null || str.isEmpty()) {
            return false; // Handle null or empty strings
        }
        return str.charAt(0) == '/';
    }


    public InlineKeyboardMarkup constructKeyboard(ArrayList<ArrayList<String>> buttonRows, int modifier) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup.builder();

        for (ArrayList<String> row : buttonRows) {
            ArrayList<InlineKeyboardButton> buttons = new ArrayList<>();
            for (String button : row) {
                buttons.add(InlineKeyboardButton.
                        builder().
                        text(button).
                        callbackData(modifier + " " + button).
                        build());
            }

            builder.keyboardRow(new InlineKeyboardRow(buttons));
        }

        return builder.build();
    }

    // Different ways to send messages
    public void sendMessage(long chatId, String text) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();
        Client client =  findClientByID(chatId);

        /*
        if ((!checkDateTime(LocalDateTime.now()) && isTimeLimiting) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because not appropriate time.");
            return;
        }

         */


        if (((!isRunning) || (isTesting)) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because bot is not working.");
            return;
        }

        try {
            telegramClient.execute(message);
            if (client != null) {
                client.setBlockedTheBot(false);
            }
        } catch (TelegramApiRequestException e) {
            if (e.getErrorCode() == 403) {
                if (client != null) {
                    //System.out.println("Client " + client.getTgNickname() + " blocked the bot. Ignore.");
                    client.setBlockedTheBot(true);
                }
            } else {
                e.printStackTrace(); // Логируем другие ошибки
            }
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Общая обработка ошибок API
        }
    }

    public void sendMessageWithLink(long chatId, String text) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();
        message.setParseMode("MarkdownV2");
        Client client =  findClientByID(chatId);

        /*
        if ((!checkDateTime(LocalDateTime.now()) && isTimeLimiting) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because not appropriate time.");
            return;
        }
        */

        if (((!isRunning) || (isTesting)) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because bot is not working.");
            return;
        }

        try {
            telegramClient.execute(message);

        } catch (TelegramApiRequestException e) {
            if (e.getErrorCode() == 403) {
                //System.out.println("Client " + client.getTgNickname() + " blocked the bot. Ignore.");
                client.setBlockedTheBot(true);
            } else {
                e.printStackTrace(); // Логируем другие ошибки
            }
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Общая обработка ошибок API
        }
    }

    public void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        Client client =  findClientByID(chatId);
        /*
        if ((!checkDateTime(LocalDateTime.now()) && isTimeLimiting) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because not appropriate time.");
            return;
        }

         */

        if (((!isRunning) || (isTesting)) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because bot is not working.");
            return;
        }

        try {
            telegramClient.execute(message);
            if (client != null) {
                client.setBlockedTheBot(false);
            }
        } catch (TelegramApiRequestException e) {
            if (e.getErrorCode() == 403) {
                //System.out.println("Client " + client.getTgNickname() + " blocked the bot. Ignore.");
                client.setBlockedTheBot(true);
            } else {
                e.printStackTrace(); // Логируем другие ошибки
            }
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Общая обработка ошибок API
        }
    }

    public void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard, String photoPath) {
        Client client =  findClientByID(chatId);
        /*
        if ((!checkDateTime(LocalDateTime.now()) && isTimeLimiting) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because not appropriate time.");
            return;
        }

        */
        if (((!isRunning) || (isTesting)) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because bot is not working.");
            return;
        }

        InputStream stream = null;
        if (isRunningFromJar()) {
            stream = Main.class.getResourceAsStream(String.format("/%s", photoPath));
        } else {
            try {
                stream = new FileInputStream(String.format("src/main/resources/%s", photoPath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        SendPhoto messageWithPhoto = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(stream, photoPath))
                .caption(text)
                .replyMarkup(keyboard)
                .build();

        try {
            telegramClient.execute(messageWithPhoto);
        } catch (TelegramApiRequestException e) {
            if (e.getErrorCode() == 403) {
                System.out.println("Пользователь " + findClientByID(chatId) + " заблокировал бота. Игнорируем.");
            } else {
                e.printStackTrace(); // Логируем другие ошибки
            }
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Общая обработка ошибок API
        }
    }


    public void sendMessage(long chatId, String text, String photoPath) {
        Client client =  findClientByID(chatId);
        /*
        if ((!checkDateTime(LocalDateTime.now()) && isTimeLimiting) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because not appropriate time.");
            return;
        }

         */

        if (((!isRunning) || (isTesting)) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
            System.out.println("Can't send message to " + client.getTgNickname() + " , because bot is not working.");
            return;
        }

        InputStream stream = null;
        if (isRunningFromJar()) {
            stream = Main.class.getResourceAsStream(String.format("/%s", photoPath));
        } else {
            try {
                stream = new FileInputStream(String.format("src/main/resources/%s", photoPath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        SendPhoto messageWithPhoto = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(stream, photoPath))
                .caption(text)
                .build();

        try {
            telegramClient.execute(messageWithPhoto);
        } catch (TelegramApiRequestException e) {
            if (e.getErrorCode() == 403) {
                System.out.println("Пользователь " + findClientByID(chatId) + " заблокировал бота. Игнорируем.");
            } else {
                e.printStackTrace(); // Логируем другие ошибки
            }
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Общая обработка ошибок API
        }
    }

    // Setting time limit for searching the courier
    public void setTimeLimitForSearch(int modifier) {
        // Запускаем задачу через рассчитанную задержку
        Runnable task = new Runnable() {
            @Override
            public void run() {
                stopCourierSearch(modifier);
            }
        };

        this.executionScheduler.schedule(task, searchingCourierTime, TimeUnit.MINUTES);
    }

    public void answerCallbackQuery(String callbackId, String text) {
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(text)
                .showAlert(false)
                .build();

        try {
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consume(Update update) {

        /*if (notificationSend == false) {
            for (Courier courier : allCouriers) {
                if (courier.getSchedule() != null) {
                    sendMessage(courier.getTgID(), "Команда QLESS приносит извинения за сбой в системе и доставленные неудобства. \n" +
                            "Техническая проблема уже устранена! \uD83D\uDE0C");
                    System.out.println("Send sorry notification to " + courier.getTgNickname());
                }
            }
            notificationSend = true;
        }
        
         */
        // If the message is custom text
        if (update.hasMessage() && update.getMessage().hasText()) {

            String text = update.getMessage().getText();
            long chatID = update.getMessage().getChatId();
            String userName = update.getMessage().getFrom().getUserName();

            // If we are provided with classroom number
            if (Main.classroomRespondWaiting.contains(chatID) && !startsWithSlash(text)) {
                Main.classroomRespondWaiting.remove(chatID);
                int orderNum = -1;

                // Finding order with appropriate client ChatID
                for (Order orders : Main.pendingOrders) {
                    if (!orders.getStatus().equals(Status.FINISHED) && !orders.getStatus().equals(Status.COURIER_NOT_FOUND) && !orders.getStatus().equals(Status.ABORTED_BY_CLIENT) && !orders.getStatus().equals(Status.ABORTED_BY_COURIER) && orders.getClient().getTgID() == chatID) { //TODO: probably not only finished, add others
                        orderNum = Main.pendingOrders.indexOf(orders);
                        break;
                    }
                }

                // Confirming classroom setting
                if (orderNum != -1) {
                    Order currentOrder = Main.pendingOrders.get(orderNum);
                    currentOrder.setNameOfClassroom(text);
                    Client currentClient = currentOrder.getClient();
                    //confirmationOfOrderDelivered(orderNum);

                    System.out.println("Order made. Searching for courier. Client: " + currentClient.getTgNickname());
                    currentOrder.setStatus(Status.COURIER_REQUESTED);
                    chooseCourierFromAll(orderNum); // TODO: move to the appropriate place
                }

            } else if (text.startsWith("/set_coins") && DB.getAllAdminsTgID().contains(chatID)) {
                try {
                    String[] splited_text = text.split(" ");
                    int token = Integer.parseInt(splited_text[2]);
                    if (splited_text[1].startsWith("@")) {
                        String tgNick = splited_text[1].substring(1);

                        Client cl = findClientByNickname(tgNick);
                        if (cl == null) {
                            sendMessage(chatID, "Error, no such client.");
                            System.out.println("Admin requested not correct nick: " + tgNick);
                            return;
                        } else {
                            int previous = cl.getNumberOfTokens();
                            cl.setNumberOfTokens(token);
                            sendMessage(chatID, "Done.");
                            boolean show = true;
                            if (splited_text.length == 4) {
                                if (splited_text[3].equals("!")) {
                                    show = false;
                                }
                            }
                            if ((token > previous) & show) {
                                sendMessage(cl.getTgID(), "\uD83E\uDD2B Команда проекта добавила тебе " + (token - previous) + " QCOIN. \n\n\uD83D\uDC8E Текущее кол-в QCOINов: " + token + ". Ты можешь сделать /order, чтобы использовать полученные QCOINы и заказать себе напиток к кабинету.");
                            }
                            System.out.println("Admin changed QCOINS of " + cl.getTgNickname() + " from " + previous + " to " + token);
                        }
                    } else {
                        sendMessage(chatID, "Error.");
                    }
                } catch (Throwable t) {
                    sendMessage(chatID, "Error.");
                    return;
                }
            }

            else if (text.equals("/statistics") & (DB.getAllAdminsTgID().contains(chatID))) {

                int num_blocked = 0;
                int num_clients = 0;
                int num_muted = 0;
                int num_tokens_0 = 0;
                int num_tokens_1 = 0;
                int num_client_orders_more_0 = 0;
                //w

                for (Client client : allClients) {
                    num_clients++;
                    if (client.getBlockedTheBot()) {
                        num_blocked++;
                    }
                    if (!client.getNotificationsAllowed()){
                        num_muted ++;
                    }
                    if (client.getNumberOfTokens() == 0) {
                        num_tokens_0++;
                    }
                    if (client.getNumberOfTokens() == 1) {
                        num_tokens_1++;
                    }
                    if (client.getNumberOfOrders() > 0){
                        num_client_orders_more_0++;
                    }
                }

                int num_couriers = 0;
                int num_scheduled_couriers = 0;
                int num_couriers_orders_more_0 = 0;

                for (Courier courier: allCouriers) {
                    num_couriers++;
                    if (courier.getSchedule() != null) {
                        num_scheduled_couriers ++;
                    }
                    if (courier.getNumberOfOrders() > 0) {
                        num_couriers_orders_more_0++;
                    }
                }
                String stat_text = "Статистика QLESS ⚡⚡\n" +
                        "\n- Кол-во зареганных клиентов: " + num_clients +
                        "\n- Кол-во клиентов, заблокировавших бот: " + num_blocked +
                        "\n- Кол-во клиентов, замьютивших уведомления о Q-erах: " + num_muted +
                        "\n- Кол-во клиентов с 0 QCOINS: " + num_tokens_0 +
                        "\n- Кол-во клиентов с 1 QCOINS: " + num_tokens_1 +
                        "\n- Кол-во клиентов, хоть раз заказавших заказ: " + num_client_orders_more_0 +
                        "\n" +
                        "\n- Кол-во зареганных Q-erов: " + num_couriers +
                        "\n- Кол-во Q-erов с заданным расписанием: " + num_scheduled_couriers +
                        "\n- Кол-во Q-erов, хоть раз доставивших заказ (с 13.02): " + num_couriers_orders_more_0;

                sendMessage(chatID, stat_text);
            }

            else if (text.equals("/check_qers")) {
                int num_of_available_couriers = Main.availableCouriers().size();
                if (num_of_available_couriers == 0) {
                    sendMessage(chatID, "⚡ Прямо сейчас нет Q-erов в кафе, но они могут появиться в ближайшие несколько минут и принять твой /order");
                } else {
                    sendMessage(chatID, "Количество Q-erов, готовых принести тебе сейчас напиток: " + num_of_available_couriers + "\n\nТы можешь воспользоваться моментом, заказав себе напиток до кабинета прямо сейчас: /order");
                }
            }


            // If we get command /cancel
            else if (text.equals("/cancel")) {
                int mod = -1;
                boolean orderFound = false;
                for (Order orders : Main.pendingOrders) {

                    boolean chatIdMatch = false;
                    Courier thisCourier = null;
                    Client thisClient = null;
                    if (orders.getCourier() != null) {
                        if (orders.getCourier().getTgID() == chatID) {
                            chatIdMatch = true;
                            thisCourier = orders.getCourier();
                        }
                    }
                    if (orders.getClient() != null) {
                        if (orders.getClient().getTgID() == chatID) {
                            chatIdMatch = true;
                            thisClient = orders.getClient();
                        }
                    }

                    if (chatIdMatch) {

                        if (!orders.isFinished()) {
                            orderFound = true;

                            // Rufuse, because the order is already paid
                            if (orders.getStatus().equals(Status.BUYING) || orders.getStatus().equals(Status.DELIVERED) || orders.getStatus().equals(Status.DELIVERY)) {
                                System.out.println(orders.getStatus());
                                if (orders.getClient().getTgID() == chatID) {
                                    sendMessage(orders.getClient().getTgID(), "Невозможно отменить заказ, так как он уже оплачен. Если возникли проблемы, ты можешь обратитья в /support");
                                    System.out.println("Client " + orders.getClient().getTgNickname() + " cannot cancel order with courier " + orders.getCourier().getTgNickname() + ", because it's paid. (status: " + orders.getStatus() + ")");
                                } else if (orders.getCourier() != null) {
                                    if (orders.getCourier().getTgID() == chatID) {
                                        sendMessage(orders.getCourier().getTgID(), "Невозможно отменить заказ, так как он уже оплачен. Если возникли проблемы, ты можешь обратитья в /support");
                                    }
                                }


                            } else {
                                mod = Main.pendingOrders.indexOf(orders);


                                if (orders.getClient().getTgID() == chatID && (orders.getStatus().equals(Status.COFFEE_PICKING) || orders.getStatus().equals(Status.CANCELLING_BY_CLIENT_COFFEE_PICKING))) {
                                    Main.pendingOrders.get(mod).setStatus(Status.CANCELLING_BY_CLIENT_COFFEE_PICKING);
                                } else if (orders.getClient().getTgID() == chatID && orders.getStatus().equals(Status.SYRUP_PICKING) || orders.getStatus().equals(Status.CANCELLING_BY_CLIENT_SYRUP_PICKING)) {
                                    Main.pendingOrders.get(mod).setStatus(Status.CANCELLING_BY_CLIENT_SYRUP_PICKING);
                                } else if (orders.getClient().getTgID() == chatID && (orders.getStatus().equals(Status.CONFIRMATION) || orders.getStatus().equals(Status.CANCELLING_BY_CLIENT_CONFIRMATION))) {
                                    Main.pendingOrders.get(mod).setStatus(Status.CANCELLING_BY_CLIENT_CONFIRMATION);
                                } else if (orders.getClient().getTgID() == chatID && (orders.getStatus().equals(Status.COURIER_REQUESTED) || orders.getStatus().equals(Status.CANCELLING_BY_CLIENT_COURIER_REQUESTED))) {
                                    Main.pendingOrders.get(mod).setStatus(Status.CANCELLING_BY_CLIENT_COURIER_REQUESTED);
                                } else if (orders.getClient().getTgID() == chatID && (orders.getStatus().equals(Status.PROVIDING_CLASSROOM) || orders.getStatus().equals(Status.CANCELLING_BY_CLIENT_PROVIDING_CLASSROOM))) {
                                    Main.pendingOrders.get(mod).setStatus(Status.CANCELLING_BY_CLIENT_PROVIDING_CLASSROOM);
                                } else if (orders.getCourier() != null) {
                                    if (orders.getCourier().getTgID() == chatID && orders.getStatus().equals(Status.CONFIRMATION)) {
                                        Main.pendingOrders.get(mod).setStatus(Status.CANCELLING_BY_COURIER_CONFIRMATION);
                                    }
                                }
                                System.out.println("Order (client: " + orders.getClient().getTgNickname() + ", courier: " + orders.getCourier() + ") almost canceled with status: " + Main.pendingOrders.get(mod).getStatus());
                                confirmCancel(mod);
                                break;
                            }
                        }
                    }
                }
                if (!orderFound) {
                    sendMessage(chatID, "У тебя нет текущего заказа, пока нечего отменять.");
                }

                // TODO: which statuses possible
            }

            // If we receive /start
            else if (text.equals("/start")) {
                
                ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                        new ArrayList<>(List.of("Заказать напиток!"))
                ));

                sendMessage(chatID, greeting(), constructKeyboard(options, -1));
                Client newClient = findClientByID(chatID);
                if (newClient == null) {
                    if (update.getMessage().getFrom().getUserName() == null) {
                        sendMessage(chatID, "К сожалению, нам не удалось получить твой ник в телеграме, так как он у тебя скрыт. Твой ник нужен нам чтобы у Q-erа была возможность связаться с тобой при доставке напитка. Попробуй открыть его в настройках, и запустить бот заново.");
                        System.out.println("User (TgID: " + chatID + ") doesn't allow his nickname to be seen and has been declined.");
                        return;
                    }
                    newClient = new Client(chatID, update.getMessage().getFrom().getUserName());
                    newClient.track();
                    System.out.println("New Client " + newClient.getTgNickname() + " initiated by start in TGBOT. Num of QCOINS: " + newClient.getNumberOfTokens());
                } else {
                    newClient.track();
                    System.out.println("Existing Client " + newClient.getTgNickname() + " initiated by start in TGBOT. Num of QCOINS: " + newClient.getNumberOfTokens());
                }

                /*
                if (!newClient.getRegistered()) {
                    int givenTokens = tokensLottery();
                    if (givenTokens == 1) {
                        sendMessage(chatID, "Поздравляем! \uD83C\uDF8A\uD83C\uDF8A\uD83C\uDF8A \nВ нашем розыгрыше ты выиграл 1 QCOIN, который ты можешь потратить на доставку себе напитка из кафе командой /order.");
                    } else if (givenTokens == 2) {
                        sendMessage(chatID, "Поздравляем!\uD83C\uDF8A\uD83C\uDF8A\uD83C\uDF8A \nВ нашем розыгрыше ты выиграл аж 2 QCOINа (!!), которые ты можешь потратить на доставку себе напитка из кафе командой /order.");
                        System.out.println("Client " + newClient.getTgNickname() + " won 2 QCOINs by start! Number of his QCOINs before it: " + newClient.getNumberOfTokens());
                    } else if (givenTokens == 3) {
                        sendMessage(chatID, "Поздравляем!\uD83C\uDF8A\uD83C\uDF8A\uD83C\uDF8A \nВ нашем розыгрыше ты выиграл аж 3 QCOINа \uD83E\uDD2F\uD83E\uDD2F, которые ты можешь потратить на доставку себе напитка из кафе командой /order.");
                        System.out.println("Client " + newClient.getTgNickname() + " won 3 QCOINs by start! Number of his QCOINs before it: " + newClient.getNumberOfTokens());
                    }
                    newClient.setNumberOfTokens(newClient.getNumberOfTokens() + givenTokens);
                    newClient.setRegistered(true);
                    System.out.println("Client " + newClient.getTgNickname() + " (tracked: " + newClient.isTracked() + ") won " + givenTokens + " QCOINs by start! Number of his QCOINs after it: " + newClient.getNumberOfTokens());
                }
                 */
            }

            // If we receive /support
            else if (text.equals("/support")) {
                sendMessage(chatID, supportMessage());
            }

            // If we receive /deliver_now
            else if (text.equals("/deliver_now")) {


                if (!isRunning && !DB.getAllTestersTgID().contains(chatID)) {
                    sendMessage(chatID, notWorking());
                    return;
                }

                if (update.getMessage().getFrom().getUserName() == null) {
                    sendMessage(chatID, "К сожалению, нам не удалось получить твой ник в телеграме, так как он у тебя скрыт. Твой ник нужен нам чтобы у клиента была возможность связаться с тобой при доставке напитка. Попробуй открыть его в настройках, и запустить бот заново.");
                    System.out.println("User (TgID: " + chatID + ") doesn't allow his nickname to be seen and has been declined.");
                    return;
                }

                if ((!checkDateTime(java.time.LocalDateTime.now()) && Main.isTimeLimiting) && (!DB.getAllAdminsTgID().contains(chatID))) {
                    //sendMessageWithLink(chatID, "\uD83D\uDE48 Кафе закрыто или близко к этому. Попробуй /deliver_now, когда кафе откроется.\uD83C\uDFA9 А чтобы ничего не пропустить, ты можешь задать промежутки времени, когда тебе будут приходить заказы (например в свои окна). Для этого заполни [форму](" + escapeMarkdownV2("https://forms.gle/jqSu39hrde17noTM9") + ")");
                    sendMessage(chatID, "\uD83D\uDE48 Кафе закрыто или близко к этому. Попробуй /deliver_now, когда кафе откроется.\uD83C\uDFA9 \n\nА чтобы ничего не пропустить, ты можешь задать промежутки времени, когда тебе будут приходить заказы (например в свои окна). Для этого заполни форму https://forms.gle/fhtS7Gn96pjhMvu4A");
                    System.out.println("User (tgID: " + chatID + ") wanted to deliver order but was declined due time.");
                    return;
                }

                if (findCourierByChatID(chatID) == null) {
                    Courier courier = new Courier(chatID, userName);
                    Main.DB.addCourierToDB(courier);
                    allCouriers.add(courier);
                }

                Courier courier = findCourierByChatID(chatID);
                Boolean prevAvailability = courier.getAvailability();
                if (findCourierByChatID(chatID).getNumberOfOrders() == 0) {
                    System.out.println("Courier with 0 orders "+ courier.getTgNickname() + " pressed deliver_now.");
                    courier.setAvailability(true);
                    Main.availableCouriers().add(courier);
                    sendMessage(chatID, "Cпасибо, что помогаешь проекту QLESS в роли Q-erа! Теперь тебе в течении " + recievingOrdersNowTime + " минут будут приходить заказы. \n\n\uD83D\uDC8E За доставку одного напитка ты получишь один QCOIN, за который ты после сможешь заказать себе напиток во время урока командой /order \uD83E\uDD2B" + "\n\nВНИМАНИЕ! Мы не рекомендуем доставлять заказы, если ты не можешь проверить факт перевода денежных средств себе на карту.", "qer_rules.jpg");
                    sendMessage(chatID, "⏳Сейчас тебе включено только временое окно доставки длиной " + recievingOrdersNowTime + " минут.\n\n\uD83D\uDE4C Ты также можешь задать промежутки времени, когда тебе будут приходить заказы (например в свои окна). Для этого заполни форму https://forms.gle/fhtS7Gn96pjhMvu4A");
                    showCurrentOrders(courier, true);
                } else if (!findCourierByChatID(chatID).getAvailability()) {
                    courier = findCourierByChatID(chatID);
                    courier.setAvailability(true);
                    Main.availableCouriers().add(courier);
                    System.out.println("Existing Courier "+ courier.getTgNickname() + " pressed deliver_now.");
                    sendMessage(chatID, "Cпасибо, что помогаешь проекту QLESS в роли Q-erа! Теперь тебе в течении " + recievingOrdersNowTime + " минут будут приходить заказы. \n\n\uD83D\uDC8E За доставку одного напитка ты получишь один QCOIN, за который ты после сможешь заказать себе напиток во время урока командой /order \uD83E\uDD2B");
                    sendMessage(chatID, "⏳Сейчас тебе включено только временое окно доставки длиной " + recievingOrdersNowTime + " минут.\n\n\uD83D\uDE4C Ты можешь задать промежутки времени, когда тебе будут приходить заказы (например в свои окна). Для этого заполни форму https://forms.gle/fhtS7Gn96pjhMvu4A");
                    System.out.println("deliver_now num of tokens of " + courier.getTgNickname() + ": " + courier.getNumberOfTokens());
                    showCurrentOrders(courier, true);
                } else if (findCourierByChatID(chatID).getAvailability()) {
                    courier = findCourierByChatID(chatID);
                    System.out.println("Courier " + courier.getTgNickname() + " with turned on availability pressed deliver_now.");
                    System.out.println("deliver_now num of tokens of " + findCourierByChatID(chatID).getTgNickname() + ": " + findCourierByChatID(chatID).getNumberOfTokens());
                    sendMessage(chatID, "Тебе уже открыта возможность принимать заказы! Cейчас тебе включено только временое окно доставки длиной " + recievingOrdersNowTime + " минут.\n\n\uD83D\uDE4C Ты также можешь задать промежутки времени, когда тебе будут приходить заказы (например в свои окна). Для этого заполни форму https://forms.gle/fhtS7Gn96pjhMvu4A");
                    showCurrentOrders(courier, true);
                }

                sendDeliverNowNotifications(courier);

                if (!prevAvailability) {
                    // Запускаем задачу через рассчитанную задержку
                    Courier finalCourier = courier;
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            turnOffAvailability(finalCourier);
                        }
                    };
                    this.executionScheduler.schedule(task, recievingOrdersNowTime, TimeUnit.MINUTES);
                }
            }


            // If we receive /do_not_disturb
            else if (text.equals("/do_not_disturb")) {
                boolean isFound = false;
                for (Courier courier : allCouriers) {
                    if (courier.getTgID() == chatID) {
                        isFound = true;
                        if (Main.findOrderByCourierOrClient(courier, null, true) == null) {
                            if (courier.getAvailability()) {
                                courier.setAvailability(false);
                                if (courier.getSchedule() != null) {
                                    sendMessage(courier.getTgID(), "Тебе не будут приходить заказы до начала следующего окна. Если ты хочешь изменить свои окна доставки, напишите об этом " + Config.getTechSupport());
                                } else {
                                    sendMessage(courier.getTgID(), "Тебе больше не будут приходить оповещения о новых заказах. Чтобы начать их получать в ближайшие " + recievingOrdersNowTime + " минут, нажми /deliver_now. \n\n\uD83D\uDE4C Ты также можешь задать промежутки времени, когда тебе будут приходить заказы (например в свои окна). Для этого заполни форму https://forms.gle/jqSu39hrde17noTM9.");
                                }
                                System.out.println("Courier " + courier.getTgNickname() + " clicked do_not_disturb.");
                            } else {
                                sendMessage(courier.getTgID(), "\uD83D\uDD12 Тебе больше не будут приходить оповещения о новых заказах. \n\nТы можешь нажать /deliver_now, если захочешь попробовать доставить напиток, чтобы потом заказать себе!");
                            }

                        } else {
                            sendMessage(courier.getTgID(), "Ты можешь отключить прием заказов только после завершения этой доставки. Если клиент еще не перевел деньги, ты можешь /cancel текущий заказ.");
                        }
                    }
                }
                if (!isFound) {
                    sendMessage(chatID, "Данная команда доступна только для Q-erов.");
                }
            }

            // if we receive /balance
            else if (text.equals("/balance")) {
                Client client = findClientByID(chatID);
                if (client != null) {
                    String balanceMessage = String.format(
                            "\uD83D\uDC8E Твой текущий баланс: " + client.getNumberOfTokens() + " QCOINов\n\n" +
                                    "1 QCOIN = 1 доставка напитка\n" +
                                    "\uD83C\uDFA9 Ты можешь получить QCOINы, доставив один заказ: /deliver_now\n" +
                                    "\uD83C\uDFA8 За имеющиеся QCOINы ты можешь заказать напиток: /order");
                    sendMessage(chatID, balanceMessage);
                } else {
                    sendMessage(chatID, "Сначала нужно начать работу с ботом через /start");
                }
            }

            // If we receive /order
            else if (text.equals("/order")) {
                order_requested(update, chatID);
            }

            else if (text.equals("/turn_off_one_day")) {
                Client client = findClientByID(chatID);
                if (client == null) {return;}
                client.setLastOrderDate(LocalDate.now());
                sendMessage(chatID, "Принято! Тебе сегодня больше не будут приходить оповещения о готовом принять заказ Q-ere. Если захочешь включить уведомления обратно, нажми /turn_on_notifications");
            }

            else if (text.equals("/turn_off_forever")) {
                Client client = findClientByID(chatID);
                if (client == null) {return;}
                client.setNotificationsAllowed(false);
                sendMessage(chatID, "Принято! Тебе больше не будут приходить оповещения о готовом принять заказ Q-ere. Если захочешь включить уведомления обратно, нажми /turn_on_notifications");
            }

            else if (text.equals("/turn_on_notifications")) {
                Client client = findClientByID(chatID);
                if (client == null) {return;}
                client.setNotificationsAllowed(true);
                client.setLastOrderDate(LocalDate.now().minusDays(1));
                sendMessage(chatID, "\uD83D\uDE09 Принято! Тебе теперь снова будут приходить уведомления о Q-ere в кафе, который готов принять твой /order");
            }

            // If we got the button response
        } else if (update.hasCallbackQuery()) {


            boolean orderNotAccepted = true;

            String rawData = update.getCallbackQuery().getData();
            String[] parts = rawData.split(" ");
            int modifier = Integer.parseInt(parts[0]);
            String data = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));

            String callbackId = update.getCallbackQuery().getId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String userName = update.getCallbackQuery().getFrom().getUserName();

            if (modifier == -1) {
                if (data.equals("Заказать напиток!")) {
                    order_requested(update, chatId);
                }

                if (data.equals("Отключить уведомления")) {
                    Client client = findClientByID(chatId);
                    sendMessage(chatId, "\n\nТы можешь отключить подобные оповещения о возможности заказа напитка:\n/turn_off_one_day - отключить на сегодня\n/turn_off_forever - отключить на всегда");
                    //System.out.println("Client " + client.getTgNickname() + " disabled notifications.");
                }
                return;
            }

            // If receives answer whether courier is available
            else if (modifier > 1000000) { // TODO: change "Да / Нет" 566239378
                Courier courierMod = Main.findCourierByID(modifier);

                if (data.equals("Да")) {
                    if (courierMod == null) {
                        System.out.println("Courier with modifier " + modifier + " answered the availability ask 'YES', but wasn't found.");
                        return;
                    }
                    courierMod.setAvailability(true);
                    sendMessage(chatId, "Принято! \nТы можешь перестать получать заказы до начала следующего окна командой /do_not_disturb");
                    /*
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            turnOffAvailability(courierMod);
                        }
                    };
                    this.executionScheduler.schedule(task, Duration.between(LocalDateTime.now(), courierMod.getNextDutyTimeEnd()).getSeconds(), TimeUnit.SECONDS);
                    */
                }

                if (data.equals("Нет")) {
                    if (courierMod == null) {
                        System.out.println("Courier with modifier " + modifier + " answered the availability ask 'NO', but wasn't found.");
                        return;
                    }
                    courierMod.setAvailability(false);
                    sendMessage(chatId, "Принято! \nЕсли у тебя сейчас нет окна и ты никогда не сможешь доставлять кофе в это время, напиши " + Config.getTechSupport() + " для изменения твоего расписания");
                    System.out.println("Courier " + courierMod.getTgNickname() + " declined receiving orders in his schedule.");
                }
            }

            // If receive answer regarding some order
            else {
                if (modifier >= pendingOrders.size()) {
                    sendMessage(chatId, "К сожалению, заказ уже истек .");
                }
                Order curOrder = Main.pendingOrders.get(modifier);
                Client curClient = curOrder.getClient();
                Courier curCourier = curOrder.getCourier();

                // If client chose a coffee
                if (menuOptions().contains(data) && curOrder.getStatus().equals(Status.COFFEE_PICKING)) {
                    curOrder.setCoffee(data);
                    sendMessage(chatId, syrupPrompt(), constructKeyboard(syrupMenu(), modifier));
                    curOrder.setStatus(Status.SYRUP_PICKING);
                }

                if (data.equals("Кофе и матча \uD83E\uDDCB") && curOrder.getStatus().equals(Status.COFFEE_PICKING)){
                    sendMessage(chatId, "\uD83E\uDDCB Выбери кофе:", constructKeyboard(coffeeMenu(), modifier));
                }

                if (data.equals("Пакетированный чай \uD83E\uDED6") && curOrder.getStatus().equals(Status.COFFEE_PICKING)){
                    sendMessage(chatId, "Все пакетированные чаи стоят 35 руб. \n\n♣\uFE0F — черный чай\n\uD83C\uDF43 — зеленый чай\n🍇 — ягодный чай\n\uD83E\uDDCA — холодный чай", constructKeyboard(teaMenu(), modifier));
                }

                if (data.equals("Другое") && curOrder.getStatus().equals(Status.COFFEE_PICKING)){
                    sendMessage(chatId, "Выбери, что ты хочешь заказать:", constructKeyboard(otherMenu(), modifier));
                }

                if (data.equals("Назад ↩\uFE0F") && curOrder.getStatus().equals(Status.COFFEE_PICKING)){
                    sendMessage(chatId, orderPrompt(), constructKeyboard(globalMenu(), modifier));
                }

                // If client chose a syrup
                if (syrupOptions().contains(data) && curOrder.getStatus().equals(Status.SYRUP_PICKING)) {
                    curOrder.setSyrup(data);
                    costOfOrder(modifier, percentOfIncome);
                    String text = "Ты заказал " + curOrder.getCoffeeName() + (curOrder.getSyrup().equals("Без сиропа") ? " " : " с сиропом ") + curOrder.getSyrup().toLowerCase() + "! Общая стоимость заказа составляет " +
                            curOrder.getFinalPrice() + " руб.";
                    sendMessage(chatId, text);
                    nameOfClassroom(modifier);
                    curOrder.setStatus(Status.PROVIDING_CLASSROOM);
                }


                // If  declined order
                if (data.equals("Нет, не могу принять заказ")) {
                    if (curOrder.getStatus().equals(Status.COURIER_REQUESTED)) {
                        int declined = Main.pendingOrders.get(modifier).getDeclined();
                        Main.pendingOrders.get(modifier).setDeclined(declined + 1);

                        /*
                        if (declined + 1 >= curOrder.getNumOfAvailableCouriers()) {
                            sendMessage(Main.pendingOrders.get(modifier).getClient().getTgID(), noCourierFound());
                            Main.pendingOrders.get(modifier).setStatus(Status.COURIER_NOT_FOUND);
                        }
                        */
                    }
                    //Courier foundCourier = findCourierByChatID(chatId);
                    sendMessage(chatId, "Принято! \nТы можешь отключить поступление новых заказов до начала следующего окна командой /do_not_disturb");
                }

                // If courier accepted order
                ArrayList<Courier> availableCouriers = Main.availableCouriers();
                if (data.equals("Да, могу принять заказ")) {
                    Courier foundCourier = Main.findCourierByID(chatId);
                    if (curOrder.getStatus().equals(Status.COURIER_REQUESTED)) {

                        curOrder.setCourier(foundCourier);
                        curOrder.setStatus(Status.CONFIRMATION);
                        //System.out.println("TRACK CANCELED HERE! Order was accepted by courier " + foundCourier.getTgNickname() + ". Num of courier QCOINS: " + foundCourier.getNumberOfTokens() + ", num of client (" + curOrder.getClient().getTgNickname() + ") QCOINS: " + curOrder.getClient().getNumberOfTokens());
                        curOrder.track();

                        Main.pendingOrders.set(modifier, curOrder);

                        orderNotAccepted = false;
                        curCourier = curOrder.getCourier();
                        curCourier.sameClient.track();
                        //System.out.println("TRACKED! Client " + curCourier.sameClient.getTgNickname() + " who is the sameclient for Courier " + curCourier.getTgNickname() + ". Client num of QCOINS: " + curCourier.sameClient.getNumberOfTokens());

                        System.out.println("Courier " + curCourier.getTgNickname() + " (track: " + curCourier.isTracked() + ") accepted order from client " + curOrder.getClient().getTgNickname() + " (track: " + curClient.isTracked() + ").");
                        costOfOrder(modifier, percentOfIncome);
                        giveInstructions(modifier);

                        /*
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                setReminderAboutOrder(modifier);
                            }
                        };
                        this.executionScheduler.schedule(task, 10, TimeUnit.MINUTES);

                         */

                    } else {
                        sendMessage(foundCourier.getTgID(), "К сожалению, этот заказ был отменен или вышло время поиска курьера.");
                    }

                }

                // If the order is confirmed by client and the client has transfered money
                if (data.equals("Я перевел деньги Q-еру!") && curOrder.getStatus().equals(Status.CONFIRMATION)) {
                    confirmationOfPurchase(modifier);
                    curOrder.setStatus(Status.BUYING);
                    sendMessage(curClient.getTgID(), "Заказ покупается...");
                }

                if (data.equals("Я получил деньги от клиента!") && curOrder.getStatus().equals(Status.CONFIRMATION)) {
                    confirmationOfPurchase(modifier);
                    curOrder.setStatus(Status.BUYING);
                    sendMessage(curClient.getTgID(), "Заказ покупается...");
                }

                // If the courier bought the order
                if (data.equals("Заказ куплен!") && curOrder.getStatus().equals(Status.BUYING)) {
                    curOrder.setStatus(Status.DELIVERY);
                    ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                            new ArrayList<>(List.of("Напиток с трубочкой доставлен!"))
                    ));
                    sendMessage(curClient.getTgID(), "Q-er купил заказ и вот-вот доставит его к кабинету " + curOrder.getNameOfClassroom() + ".");
                    sendMessage(curCourier.getTgID(), "Последний шаг! Возьми трубочку (но не открвай её) и доставь напиток в кабинет " + curOrder.getNameOfClassroom(), constructKeyboard(options, modifier));
                    // add confirmation of class
                    //confirmationOfOrderDelivered(modifier);
                }

                // If the courier delivered the order
                if (data.equals("Напиток с трубочкой доставлен!") && curOrder.getStatus().equals(Status.DELIVERY)) {
                    Order order = Main.pendingOrders.get(modifier);
                    order.setStatus(Status.DELIVERED);
                    confirmationOfOrderReceived(modifier);

                    sendMessage(order.getCourier().getTgID(), "Принято! \n\n✉\uFE0F Мы уведомили клиента, что ты на месте. Пожалуйста, подожди клиента минимум минуту. После ты можешь оставить напиток около кабинета и идти, только отправь клиенту (@" + order.getClient().getTgNickname() + ") фотографию, где стоит напиток. \n\n\uD83E\uDD1D Если клиент уже забрал напиток, напиши ему, чтобы он нажал в боте кнопку 'Я получил заказ!', чтобы завершить заказ. ");
                }

                // If client received the order
                if (data.equals("Я получил заказ!") && curOrder.getStatus().equals(Status.DELIVERED)) {
                    Order order = Main.pendingOrders.get(modifier);
                    transferTokens(order);

                    order.setStatus(Status.FINISHED);

                    System.out.println("Order is finished. Client: " + order.getClient().getTgNickname() + " (track: " + order.getClient().isTracked() + "), Courier: " + order.getCourier().getTgNickname() + " (track: " + order.getCourier().isTracked() + ")");
                    curClient.setLastOrderDate(LocalDate.now());

                    String url = "https://forms.gle/7Drq1f1VbuysH1D28";
                    sendMessage(curClient.getTgID(), "\uD83E\uDD73 Заказ завершен! Спасибо за пользование нашим сервисом! \n\nПодписывайся на наш канал, чтобы оставаться в курсе развития проекта " + Config.getProjectUpdates());
                    sendMessage(curClient.getTgID(), "\uD83E\uDEF6 Мы только начинаем нашу работу и хотим стать лучше. Заполни, пожалуйста, форму с обратной связью: " + url);

                    if (curCourier.getNumberOfOrders() == 1) {
                        sendMessage(curCourier.getTgID(), "Клиент забрал напиток! Заказ завершен! \n\n\uD83D\uDC8E Это был твой первый заказ, поэтому вместо одного QCOINа, тебе начислилось два. Текущее кол-во QCOINов: " + curCourier.getNumberOfTokens() + ". \nQCOINы дают тебе возможность заказать напиток из кафе. Для этого нажми /order \n\n\uD83D\uDC49 Если бы ты хотел доставить еще напиток, нажми /deliver_now");
                    } else {
                        sendMessage(curCourier.getTgID(), "Клиент забрал напиток! Заказ завершен! \n\n\uD83D\uDC8E Тебе начислен 1 QCOIN, текущее кол-во QCOINов: " + curCourier.getNumberOfTokens() + ". \nQCOINы дают тебе возможность заказать напиток из кафе. Для этого нажми /order \n\n\uD83D\uDC49 Если бы ты хотел доставить еще напиток, нажми /deliver_now");

                    }
                    sendMessage(curCourier.getTgID(), "\uD83E\uDEF6 Подписывайся на канал сервиса, чтобы оставаться в курсе развития проекта " + Config.getProjectUpdates());
                }

                // If client didn't receive the order
                if (data.equals("Заказ не доставлен(") && curOrder.getStatus().equals(Status.DELIVERED)) {
                    curClient = curOrder.getClient();
                    String text = "Без паники! Скорее всего вы с Q-erом просто не нашли друг друга. Пожалуйста, спроси у него, где он оставил заказ (tg: @" + curOrder.getCourier().getTgNickname() + ")" + "\n" +
                            "Если ты так и не получишь свой напиток, обратись в поддержку: /support";
                    ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                            new ArrayList<>(List.of("Я получил заказ!"))
                    ));
                    sendMessage(curClient.getTgID(), text, constructKeyboard(options, modifier));
                    String text_courier = "Клиент не нашел заказ. Если ты ушел, пожалуйста, вернись к кабинету " + curOrder.getNameOfClassroom() + " или спишись с клиентом, пояснив, где ты оставил напиток (tg: @" + curOrder.getClient().getTgNickname() + ")." + "\n\n" + "При возникновении проблем, обращайся в поддержку командой /support";
                    sendMessage(curCourier.getTgID(), text_courier);
                }


                // If wants to cancel order
                if (data.equals("Отменить заказ")) {
                    if (curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_PROVIDING_CLASSROOM) || curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_CONFIRMATION) || curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_COURIER_REQUESTED) || curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_COFFEE_PICKING) || curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_SYRUP_PICKING)) {
                        curOrder.getClient().abortOrder();// TODO: check method in Client
                        Main.pendingOrders.get(modifier).setStatus(Status.ABORTED_BY_CLIENT);
                        if (curCourier != null) {
                            sendMessage(curOrder.getCourier().getTgID(), "К сожалению, заказ был отменен.");
                        }
                        System.out.println("Client " + curOrder.getClient().getTgNickname() + " cancelled the order.");
                        sendMessage(curOrder.getClient().getTgID(), "Ты отменил заказ. У тебя не спишутся QCOINы. Ты можешь сделать еще один /order");
                    } else if (curOrder.getStatus().equals(Status.CANCELLING_BY_COURIER_CONFIRMATION)) {
                        curOrder.getCourier().abortOrder();
                        Main.pendingOrders.get(modifier).setStatus(Status.ABORTED_BY_COURIER);
                        System.out.println("Courier " + curOrder.getCourier().getTgNickname() + " cancelled the order.");
                        sendMessage(curOrder.getClient().getTgID(), "К сожалению, заказ был отменен. Попробуй сделать еще один /order");
                        sendMessage(curOrder.getCourier().getTgID(), "Ты отменил заказ. Ты можешь сделать еще один /order");
                    }
                }

                // If does not want to cancel order
                if (data.equals("Нет, оставить заказ")) {
                    if (curOrder.getStatus().equals(Status.CANCELLING_BY_COURIER_CONFIRMATION) || curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_CONFIRMATION)) {
                        curOrder.setStatus(Status.CONFIRMATION);
                    } else if (curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_COFFEE_PICKING)) {
                        curOrder.setStatus(Status.COFFEE_PICKING);
                    } else if (curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_SYRUP_PICKING)) {
                        curOrder.setStatus(Status.SYRUP_PICKING);
                    } else if (curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_COURIER_REQUESTED)) {
                        curOrder.setStatus(Status.COURIER_REQUESTED);
                    } else if (curOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_PROVIDING_CLASSROOM)) {
                        curOrder.setStatus(Status.PROVIDING_CLASSROOM);

                    }
                }
            }
            answerCallbackQuery(callbackId, "Принято!");
        }
    }

    public void showCurrentOrders(Courier courier, Boolean isFromDeliverNow) {
        if ((!checkDateTime(LocalDateTime.now()) && (!DB.getAllAdminsTgID().contains(courier.getTgID())))) {
            return;
        }

        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Да, могу принять заказ")),
                new ArrayList<>(List.of("Нет, не могу принять заказ"))
        ));

        ArrayList<Order> possibleOrders = new ArrayList<>();

        int numberOfOrders = 0;
        for (Order order : pendingOrders) {
            if (order.getStatus() == Status.COURIER_REQUESTED) {
                boolean isClientForAnotherOrder = false;
                for (Order orders : pendingOrders) {
                    if (orders.getClient() != null) {
                        if (orders.getClient().getTgID() == courier.getTgID() && !orders.isFinished()) {
                            isClientForAnotherOrder = true;
                        }
                    }
                }

                if (courier.getTgID() != order.getClient().getTgID() && !isClientForAnotherOrder) {

                    numberOfOrders += 1;
                    if (isFromDeliverNow) {
                        String lastSentence = "";
                        if (courier.getNumberOfOrders() == 0 && isOneMoreTokenForFirstOrder) {
                            lastSentence = "\n\nЭто будет твой первый заказ, поэтому за доставку ты получишь два QCOINа, вместо одного! \uD83D\uDE1C\n\nМожешь ли ты принять заказ?";
                        } else {
                            lastSentence = "\n\nЗа доставку этого напитка, ты получишь 1 QCOIN, за который сможешь заказать напиток и себе \uD83D\uDE1C\n\nМожешь ли ты принять заказ?";
                        }
                        sendMessage(courier.getTgID(), "☕\uFE0F Новый заказ! \nНапиток: " + order.getCoffeeName() + "\nСироп: " + order.getSyrup() +
                                "\nКабинет: " + order.getNameOfClassroom() + lastSentence, constructKeyboard(options, order.getModifier()));

                        order.setNumOfAvailableCouriers(order.getNumOfAvailableCouriers() + 1);
                    } else {
                        possibleOrders.add(order);
                    }
                }


            }
        }

        if (!isFromDeliverNow) {
            Random random = new Random();
            // Выбираем случайный индекс от 0 до list.size() - 1
            int randomIndex = random.nextInt(possibleOrders.size());
            // Получаем элемент по случайному индексу
            Order randomOrder = possibleOrders.get(randomIndex);

            String lastSentence = "";
            if (courier.getNumberOfOrders() == 0 && isOneMoreTokenForFirstOrder) {
                lastSentence = "Это будет твой первый заказ, поэтому за доставку ты получишь два QCOINа, вместо одного!";
            }

            sendMessage(courier.getTgID(), "☕\uFE0F Привет! Есть возможность доставить заказ, чтобы получить возможность заказать самому! " + lastSentence +"\n\nНапиток: " + randomOrder.getCoffeeName() + "\nСироп: " + randomOrder.getSyrup() +
                    "\nКабинет: " + randomOrder.getNameOfClassroom() + "\nМожешь ли ты принять заказ?", constructKeyboard(options, randomOrder.getModifier()));
            randomOrder.setNumOfAvailableCouriers(randomOrder.getNumOfAvailableCouriers() + 1);
            courier.setLastSendNotification(LocalDateTime.now());
        }

        if (numberOfOrders == 0 && isFromDeliverNow) {
            sendMessage(courier.getTgID(), "\uD83D\uDC40 На данный момент, заказов без Q-era не нашлось. Мы тебе напишем, если они появятся.");
        }


    }

    public void turnOffAvailability(Courier courier) {
        courier.setAvailability(false);
        System.out.println("Availability of " + courier.getTgNickname() + " turned off automaticaly after deliver_now");
    }

    public void chooseCourier(int modifier) {
        ArrayList<Courier> availableCouriers = Main.availableCouriers();
        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Да, могу принять заказ")),
                new ArrayList<>(List.of("Нет, не могу принять заказ"))
        ));
        Order order = Main.pendingOrders.get(modifier);

        int numOfAvailableCouriers = 0;
        for (Courier courier : availableCouriers) {
            if (checkDateTime(LocalDateTime.now()) || DB.getAllAdminsTgID().contains(courier.getTgID())) {
                boolean isClientForAnotherOrder = false;
                for (Order orders : pendingOrders) {
                    if (orders.getClient() != null) {
                        if (orders.getClient().getTgID() == courier.getTgID() && !orders.isFinished()) {
                            isClientForAnotherOrder = true;
                        }
                    }
                }
                if (courier.getTgID() != order.getClient().getTgID() && !isClientForAnotherOrder) {
                    numOfAvailableCouriers++;
                    String lastSentence = "";
                    if (courier.getNumberOfOrders() == 0 && isOneMoreTokenForFirstOrder) {
                        lastSentence = "\n\nЭто будет твой первый заказ, поэтому за доставку ты получишь два QCOINа, вместо одного! \uD83D\uDE1C\n\nМожешь ли ты принять заказ?";
                    } else {
                        lastSentence = "\n\nЗа доставку этого напитка, ты получишь 1 QCOIN, за который сможешь заказать напиток и себе \uD83D\uDE1C\n\nМожешь ли ты принять заказ?";
                    }
                    sendMessage(courier.getTgID(), "☕\uFE0F Новый заказ! \nНапиток: " + order.getCoffeeName() + "\nСироп: " + order.getSyrup() +
                            "\nКабинет: " + order.getNameOfClassroom() + lastSentence, constructKeyboard(options, order.getModifier()));
                }
            }
        }
        order.setNumOfAvailableCouriers(numOfAvailableCouriers);

        /*
        if (numOfAvailableCouriers == 0) {
            sendMessage(order.getClient().getTgID(), "К сожалению, сейчас нет свободных Q-erов. Попробуйте сделать /order чуть позже.");
            order.setStatus(Status.COURIER_NOT_FOUND);
        */

        String text = "Принято! Ищем тебе свободного Q-erа. Это займет не больше пяти минут. \nКол-во доступных сейчас Q-erов: " + numOfAvailableCouriers;
        if (numOfAvailableCouriers == 0) {
            text += ", но они могут появиться в ближайшее время.";
        }
        sendMessage(order.getClient().getTgID(), text);
        setTimeLimitForSearch(modifier);
    }

    public void scheduleNextWindow(String breaksJsonFile) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Читаем JSON-файл в Map, где ключ — день недели в виде строки, значение — список пар времени
            Map<String, List<List<String>>> rawData = mapper.readValue(
                    new File(breaksJsonFile),
                    new TypeReference<Map<String, List<List<String>>>>() {}
            );

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextStart = null;
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            // Ищем ближайший момент начала окна в ближайшие 7 дней (включая сегодня)
            for (int offset = 0; offset < 7; offset++) {
                LocalDate day = LocalDate.now().plusDays(offset);
                // В Java DayOfWeek: понедельник = 1, ..., воскресенье = 7.
                // Приводим к ключу от "0" до "6" (0 = понедельник)
                int dayIndex = day.getDayOfWeek().getValue() - 1;
                String dayKey = String.valueOf(dayIndex);
                List<List<String>> dayRanges = rawData.getOrDefault(dayKey, Collections.emptyList());

                for (List<String> timePair : dayRanges) {
                    if (timePair.size() == 2) {
                        // Парсим время начала окна
                        LocalTime startTime = LocalTime.parse(timePair.get(0), timeFormatter);
                        LocalDateTime scheduledDateTime = LocalDateTime.of(day, startTime);
                        if (scheduledDateTime.isAfter(now)) {
                            if (nextStart == null || scheduledDateTime.isBefore(nextStart)) {
                                nextStart = scheduledDateTime;
                            }
                        }
                    }
                }
                // Если для данного дня найдено окно, можно остановить поиск
                if (nextStart != null && nextStart.toLocalDate().equals(day)) {
                    break;
                }
            }

            if (nextStart == null) {
                return;
            }

            // Вычисляем задержку до следующего запуска
            long delaySeconds = ChronoUnit.SECONDS.between(now, nextStart);
            System.out.println("Запланировано выполнение x() на " + nextStart + " (через " + delaySeconds + " мс)");

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    for (Courier courier : Main.availableCouriers()) {
                        if (courier.isNotificationsAllowed() && courier.isNotificationOK()){
                            showCurrentOrders(courier, false);
                        }
                    }
                }
            };
            this.executionScheduler.schedule(task, delaySeconds, TimeUnit.SECONDS);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void chooseCourierFromAll(int modifier) {

        ArrayList<Courier> availableCouriers = Main.availableCouriers();
        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Да, могу принять заказ")),
                new ArrayList<>(List.of("Нет, не могу принять заказ"))
        ));
        Order order = Main.pendingOrders.get(modifier);

        int numOfAvailableCouriers = 0;
        for (Courier courier : allCouriers) {
            if (checkDateTime(LocalDateTime.now()) || DB.getAllAdminsTgID().contains(courier.getTgID())) {
                boolean isClientForAnotherOrder = false;
                for (Order orders : pendingOrders) {
                    if (orders.getClient() != null) {
                        if (orders.getClient().getTgID() == courier.getTgID() && !orders.isFinished()) {
                            isClientForAnotherOrder = true;
                        }
                    }
                }

                if (courier.getAvailability() || (Main.isBreakNow() && isUsingBreaks)){
                    if (courier.getTgID() != order.getClient().getTgID() && !isClientForAnotherOrder) {
                        if (courier.isNotificationOK() || courier.getAvailability()) {
                            numOfAvailableCouriers++;
                            if (!courier.getAvailability()) {
                                String lastSentence = "";
                                if (courier.getNumberOfOrders() == 0 && isOneMoreTokenForFirstOrder) {
                                    lastSentence = "Это будет твой первый заказ, поэтому за доставку ты получишь два QCOINа, вместо одного!";
                                }

                                sendMessage(courier.getTgID(), "☕\uFE0F Привет! Есть возможность доставить заказ, чтобы получить возможность заказать самому! " + lastSentence + "\n\nНапиток: " + order.getCoffeeName() + "\nСироп: " + order.getSyrup() +
                                        "\nКабинет: " + order.getNameOfClassroom() + "\nМожешь ли ты принять заказ?", constructKeyboard(options, modifier));
                            } else {
                                String lastSentence = "";
                                if (courier.getNumberOfOrders() == 0 && isOneMoreTokenForFirstOrder) {
                                    lastSentence = "\n\nЭто будет твой первый заказ, поэтому за доставку ты получишь два QCOINа, вместо одного \uD83D\uDE1C\n\nМожешь ли ты принять заказ?";
                                } else {
                                    lastSentence = "\n\nЗа доставку этого напитка, ты получишь 1 QCOIN, за который сможешь заказать напиток и себе \uD83D\uDE1C\n\nМожешь ли ты принять заказ?";
                                }
                                sendMessage(courier.getTgID(), "☕\uFE0F Новый заказ! \nНапиток: " + order.getCoffeeName() + "\nСироп: " + order.getSyrup() +
                                        "\nКабинет: " + order.getNameOfClassroom() + lastSentence, constructKeyboard(options, order.getModifier()));

                            }
                            courier.setLastSendNotification(LocalDateTime.now());
                        }
                    }
                }
            }
        }
        order.setNumOfAvailableCouriers(numOfAvailableCouriers);

        /*
        if (numOfAvailableCouriers == 0) {
            sendMessage(order.getClient().getTgID(), "К сожалению, сейчас нет свободных Q-erов. Попробуйте сделать /order чуть позже.");
            order.setStatus(Status.COURIER_NOT_FOUND);
        */

        String text = "Принято! Ищем тебе свободного Q-erа. Это займет несколько минут. \nМы показали твой заказ : " + numOfAvailableCouriers + " Q-erам и попробуем найти еще нескольких";
        if (numOfAvailableCouriers == 0) {
            text = "\uD83D\uDD0D Принято! Ищем тебе свободного Q-erа. Это займет несколько минут ";
        }
        sendMessage(order.getClient().getTgID(), text);
        setTimeLimitForSearch(modifier);
    }

    public void transferTokens(Order order) {
        Client client = order.getClient();
        Courier courier = order.getCourier();


        client.setNumberOfTokens(client.getNumberOfTokens() - 1);
        System.out.println("Client " + client.getTgNickname() + " spent 1 QCOIN. New balance: " + client.getNumberOfTokens());

        if (courier.getNumberOfTokens() == 0) {
            courier.setNumberOfTokens(courier.getNumberOfTokens() + 2);
            System.out.println("Courier " + courier.getTgNickname() + " earned 2 QCOIN, because of first order. New balance: " + courier.getNumberOfTokens());
        } else {
            courier.setNumberOfTokens(courier.getNumberOfTokens() + 1);
            System.out.println("Courier " + courier.getTgNickname() + " earned 1 QCOIN. New balance: " + courier.getNumberOfTokens());
        }
    }


    public void askCourierAvailability(Courier courier, LocalDateTime time) {
        if (isRunning) {
            ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                    new ArrayList<>(List.of("Да")),
                    new ArrayList<>(List.of("Нет"))
            ));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");


            String prompt = "Привет! Согласно нашим данным, в " + time.toLocalTime().format(formatter) + " у тебя началось окно. \nСмог бы ты принимать заказы до конца окна? ";
            sendMessage(courier.getTgID(), prompt, constructKeyboard(options, (int) courier.getTgID()));

            scheduledCouriers.remove(courier.getTgID());

            courier.planAskingAvailability();
        }

    }

    public int tokensLottery() {
        double randomNumber = Math.random();
        double prev = 0;
        int givenTokens = 0;
        for (double[] tokensChance : tokensChances) {
            double chance = tokensChance[0];
            int num = (int) tokensChance[1];
            if (randomNumber >= prev && randomNumber < prev + chance) {
                givenTokens = num;
            }
            prev = chance + prev;
        }
        return givenTokens;
    }

    public void giveTokenIfFirstTimeCourier(Courier courier) {
        if (courier.getNumberOfOrders() == 0) {
            courier.setNumberOfTokens(courier.getNumberOfTokens() + 1);
            sendMessage(courier.getTgID(), "Ты доставил свой первый заказ, поэтому вместо 1 QCOINа, тебе начислялось два.");
        }
    }

    public void giveInstructions(int modifier) {
        Order currentOrder = Main.pendingOrders.get(modifier);
        Courier currentCourier = currentOrder.getCourier();
        Client currentClient = currentOrder.getClient();
        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Я перевел деньги Q-еру!"))
        ));

        ArrayList<ArrayList<String>> optionsForCourier = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Я получил деньги от клиента!"))
        ));

        String courierInstructions = "Заказ принят!" + "\n" +
                "Телеграм клиента: @" + currentClient.getTgNickname() + "\n" + "\n" +
                "Теперь спроси в кафе, есть ли у них " + currentOrder.getCoffeeName() + (currentOrder.getSyrup().equals("Без сиропа") ? " " : " с сиропом ") + currentOrder.getSyrup().toLowerCase() + ".\n" +
                "\uD83D\uDE45\u200D♂\uFE0F Если нет, попроси клиента выбрать другой напиток." + "\n" +
                "✅ Если есть, пришли клиенту номер, куда ему переводить деньги.\n" + "\n" +
                "По ценам кафе, заказ стоит " + currentOrder.getFinalPrice() + " руб." +
                "\n\nКогда клиент переведет тебе " + currentOrder.getFinalPrice() + " руб." + ", нажми кнопку ниже ⬇\uFE0F";

        String clientInstructions = "Q-er найден!" + "\n" +
                "Телеграм курьера: @" + currentCourier.getTgNickname() + "\n" + "\n" +
                "☎\uFE0F Пожалуйста, свяжись с Q-erом, чтобы убедиться в том, что в кафе есть твой напиток и сироп. Если нужно, напиши ему захватить сахар или корицу. Q-er пришлет тебе, куда переводить деньги за заказ." + "\n" + "\n" +
                "Переведи курьеру полную стоимость заказа в " + currentOrder.getFinalPrice() + " руб." + "\n\n" +
                "\uD83D\uDC8E За этот заказ у тебя спишется 1 QCOIN, текущее кол-во QCOINов: " + currentClient.getNumberOfTokens() + "\n" +
                "Когда переведешь Q-erу деньги за заказ, нажми эту кнопку ⬇\uFE0F";

        if (currentCourier.getNumberOfOrders() < 5) {
            sendMessage(currentCourier.getTgID(), courierInstructions, constructKeyboard(optionsForCourier, modifier), "qer_instructions.jpg");
        } else {
            sendMessage(currentCourier.getTgID(), courierInstructions, constructKeyboard(optionsForCourier, modifier));
        }

        sendMessage(currentClient.getTgID(), clientInstructions, constructKeyboard(options, modifier));
    }

    public void stopCourierSearch(int modifier) {
        if (Main.pendingOrders.get(modifier).getStatus() == Status.COURIER_REQUESTED || Main.pendingOrders.get(modifier).getStatus() == Status.CANCELLING_BY_CLIENT_COURIER_REQUESTED) {
            Order currentOrder = Main.pendingOrders.get(modifier);
            Client currentClient = currentOrder.getClient();
            currentOrder.setStatus(Status.COURIER_NOT_FOUND);
            String text = "К сожалению, не один из доступных Q-erов не смог принять твой заказ. Попробуй сделать /order чуть позже.";
            sendMessage(currentClient.getTgID(), text);
        }
    }

    public void confirmationOfOrderDelivered(int modifier) {
        Order currentOrder = Main.pendingOrders.get(modifier);
        Courier currentCourier = currentOrder.getCourier();
        Client currentClient = currentOrder.getClient();
        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Напиток с трубочкой доставлен!"))
        ));
        sendMessage(currentCourier.getTgID(), "Доставлен ли заказ для " + currentClient.getTgNickname() + "?", constructKeyboard(options, modifier));
    }

    public void confirmationOfOrderReceived(int modifier) {
        Order currentOrder = Main.pendingOrders.get(modifier);
        Courier currentCourier = currentOrder.getCourier();
        Client currentClient = currentOrder.getClient();
        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Я получил заказ!")),
                new ArrayList<>(List.of("Заказ не доставлен("))
        ));
        sendMessage(currentClient.getTgID(), "Q-er (@" + currentCourier.getTgNickname() + ") доставил твой заказ до двери кабинета! Пожалуйста, забери его, пока он не ушел. Q-er обычно ждет не больше минуты, а затем оставляет напиток у кабинета. \n\nКогда ты получишь напиток, нажми кнопку ниже ⬇\uFE0F", constructKeyboard(options, modifier));
    }

    public void setReminderAboutOrder(int modifier) {
        Order order = Main.pendingOrders.get(modifier);
        if (!order.isFinished()) {
            if ((order.getStatus() == Status.CONFIRMATION || order.getStatus() == Status.DELIVERED) || (order.getStatus() == Status.CANCELLING_BY_COURIER_CONFIRMATION)) {
                Client client = order.getClient();
                ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                        new ArrayList<>(List.of("Я получил заказ!")),
                        new ArrayList<>(List.of("Заказ не доставлен("))
                ));
                sendMessage(client.getTgID(), "Уже долго ничего не слышно про твой заказ. Подскажи, получил ли ты свой натипок?", constructKeyboard(options, modifier));
            }

        }
    }

    public void costOfOrder(int modifier, int percent) {
        Order currentOrder = Main.pendingOrders.get(modifier);
        Courier currentCourier = currentOrder.getCourier();
        Client currentClient = currentOrder.getClient();
        String syrup = currentOrder.getSyrup();
        String coffee = currentOrder.getCoffee();

        HashMap<String, Integer> priceList = new HashMap<>();
        priceList.put("Эспрессо: 80 р.", 80);
        priceList.put("Двойной эспрессо: 160 р.", 160);
        priceList.put("Американо: 85 р.", 85);
        priceList.put("Айс американо: 85 р.", 85);
        priceList.put("Латте: 150 р.", 150);
        priceList.put("Айс латте: 165 р.", 165);
        priceList.put("Капучино: 120 р.", 120);
        priceList.put("Айс капучино: 120 р.", 120);
        priceList.put("Большой капучино: \n165 р.", 165);
        priceList.put("Большой айс капучино: \n165 р.", 165);
        priceList.put("Раф: 170 р.", 170);
        priceList.put("Айс раф: 170 р.", 170);
        priceList.put("Горячий шоколад: 185 р.", 185);
        priceList.put("Какао: 100 р.", 100);
        priceList.put("Глясе: 185 р.", 185);
        priceList.put("Флэт-уайт: 125 р.", 125);
        priceList.put("Айс флэт-уайт: 125 р.", 125);
        priceList.put("Матча: 180 р.", 180);
        priceList.put("Айс матча: 180 р.", 180);

        double price = 0.0;
        if (!syrup.equals("Без сиропа")) {
            price = price + 35;
        }

        if (teaOptionsOnly().contains(coffee)) {
            price += 35;
        } else {
            price = price + priceList.get(coffee);
        }

        // price = price * (1 + (double) percent / 100);

        currentOrder.setFinalPrice((int) price);
    }
    // TODO: COST of double espresso

    public void confirmationOfPurchase(int modifier) {
        Order currentOrder = Main.pendingOrders.get(modifier);
        Courier currentCourier = currentOrder.getCourier();
        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Заказ куплен!"))
        ));
        sendMessage(currentCourier.getTgID(), "\uD83D\uDCB8 Клиент перевел тебе деньги. Проверь и покупай заказ. ", constructKeyboard(options, modifier));
    }

    public void nameOfClassroom(int modifier) {
        Order currentOrder = Main.pendingOrders.get(modifier);
        Client currentClient = currentOrder.getClient();
        Main.classroomRespondWaiting.add(currentClient.getTgID());
        sendMessage(currentClient.getTgID(), "В какой кабинет доставить заказ?");
    }

    public void confirmClassroom(int modifier) {
        Order currentOrder = Main.pendingOrders.get(modifier);
        Client currentClient = currentOrder.getClient();
        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Да, это мой кабинет")),
                new ArrayList<>(List.of("Нет, это не мой кабинет"))
        ));
        sendMessage(currentClient.getTgID(), "Твой кабинет: " + currentOrder.getNameOfClassroom() + "?", constructKeyboard(options, modifier)); // if not, ask again
    }

    public void sendDeliverNowNotifications(Courier courier) {

        for (Client client : allClients) {
            if (client.getTgNickname().equalsIgnoreCase("ivanfenster") || client.getTgNickname().equalsIgnoreCase("merek_taliso")) {
                System.out.println("Bot wants to send message to" + client.getTgNickname());
                System.out.println("Tokens: " + client.getNumberOfTokens() + ", не курьер: " + (client.getTgID() != courier.getTgID()) + ", other order:" + findOrderByCourierOrClient(null, client, true) + ", notifications allowed: " + client.getNotificationsAllowed() + ", notificagtion ok: " + client.isNotificationOK());
            }
            if (!checkDateTime(LocalDateTime.now()) && (!DB.getAllAdminsTgID().contains(client.getTgID()))) {
                continue;
            }
            if ((client.getNumberOfTokens() > 0) && (client.getTgID() != courier.getTgID()) && (findOrderByCourierOrClient(null, client, true) == null) && (client.getNotificationsAllowed()) && (client.isNotificationOK())) {
                if (client.getLastOrderDate() != null) {
                    if (client.getLastOrderDate().isEqual(LocalDate.now())) {
                        continue;
                    }
                }
                ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                        new ArrayList<>(List.of("Заказать напиток!")),
                        new ArrayList<>(List.of("Отключить уведомления"))
                ));

                sendMessage(client.getTgID(), "Привет! \n\n\uD83D\uDD4A\uD83E\uDD2B В кафе есть Q-er, который готов сейчас доставить тебе напиток. У тебя есть QCOIN! Ты можешь воспользоваться этой возможностью, для этого нажми /order", constructKeyboard(options, -1));
                client.setLastSendNotification(LocalDateTime.now());
            }
        }
    }

    public void confirmCancel(int modifier) {
        Order currentOrder = Main.pendingOrders.get(modifier);
        ArrayList<ArrayList<String>> options = new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Отменить заказ")),
                new ArrayList<>(List.of("Нет, оставить заказ"))
        ));
        long chatID = 0;
        if (currentOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_COFFEE_PICKING) || currentOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_SYRUP_PICKING) || currentOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_CONFIRMATION) || currentOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_COURIER_REQUESTED) || currentOrder.getStatus().equals(Status.CANCELLING_BY_CLIENT_PROVIDING_CLASSROOM)) {
            chatID = currentOrder.getClient().getTgID();
        } else if (currentOrder.getStatus().equals(Status.CANCELLING_BY_COURIER_CONFIRMATION)) {
            chatID = currentOrder.getCourier().getTgID();
        }
        sendMessage(chatID, "Ты правда хочешь отменить заказ?", constructKeyboard(options, modifier));
    }

    private boolean checkDateTime(java.time.LocalDateTime dateTime) {
        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now();
        DayOfWeek dayOfWeek = DayOfWeek.from(localDate);
        int dayOfWeekInt = dayOfWeek.getValue() - 1;
        for (int i = 0; i < acceptedTime.get(dayOfWeekInt).size(); i++) {
            ArrayList<LocalTime> timeInterval = acceptedTime.get(dayOfWeekInt).get(i);
            if (localTime.isAfter(timeInterval.get(0)) && localTime.isBefore(timeInterval.get(1))) {
                return true;
            }
        }
        return false;
    }

    private void order_requested(Update update, long chatID){
        if (!isRunning && !DB.getAllTestersTgID().contains(chatID)) {
            sendMessage(chatID, notWorking());
            return;
        }

        if (!checkDateTime(java.time.LocalDateTime.now()) && Main.isTimeLimiting && (!DB.getAllAdminsTgID().contains(chatID))) {
            sendMessage(chatID, notApproriateTimeRefuse());
            System.out.println("User (tgID: " + chatID + ") wanted to create order but was declined due time.");
            return;
        }

        boolean isClientForAnyOrder = false;
        boolean isCourierForAnyOrder = false;

        for (Order orders : Main.pendingOrders) {
            if (orders.getCourier() != null) {
                if (orders.getCourier().getTgID() == chatID && !orders.isFinished()) {
                    isCourierForAnyOrder = true;
                }
            }
            if ((orders.getClient().getTgID() == chatID) && (!orders.getStatus().equals(Status.DECLINED)) && (!orders.getStatus().equals(Status.FINISHED)) && (!orders.getStatus().equals(Status.ABORTED_BY_CLIENT)) && (!orders.getStatus().equals(Status.ABORTED_BY_COURIER) && (!orders.getStatus().equals(Status.COURIER_NOT_FOUND)))) {
                isClientForAnyOrder = true;
                break;
            }
        }

        if (isClientForAnyOrder) {
            sendMessage(chatID, "На данной стадии проекта, ты не можешь создать два заказа одновременно. Чтобы отменить заказ, введи /cancel");
        } else if (isCourierForAnyOrder) {
            sendMessage(chatID, "Сначала заверши доставку заказа.");
        } else {
            Client newClient = findClientByID(chatID);
            if (newClient == null) {
                if (update.getMessage().getFrom().getUserName() == null) {
                    sendMessage(chatID, "К сожалению, нам не удалось получить твой ник в телеграме, так как он у тебя скрыт. Твой ник нужен нам чтобы у Q-erа была возможность связаться с тобой при доставке напитка. Попробуй открыть его в настройках, и запустить бот заново.");
                    System.out.println("User (TgID: " + chatID + ") doesn't allow his nickname to be seen and has been declined.");
                    return;
                }
                newClient = new Client(chatID, update.getMessage().getFrom().getUserName());
                newClient.track();
                System.out.println("TRACKED! New Client " + newClient.getTgNickname() + " initiated by order in TGBOT. Num of QCOINS: " + newClient.getNumberOfTokens());
            } else {
                newClient.track();
                System.out.println("TRACKED! Existing Client " + newClient.getTgNickname() + " initiated by order in TGBOT. Num of QCOINS: " + newClient.getNumberOfTokens());
            }

            if (!newClient.getRegistered()) {
                int givenTokens = tokensLottery();
                if (givenTokens == 1) {
                    sendMessage(chatID, "Поздравляем! \uD83C\uDF8A\uD83C\uDF8A\uD83C\uDF8A \nВ нашем розыгрыше ты выиграл 1 QCOIN, который ты можешь потратить на доставку себе напитка из кафе командой /order.");
                } else if (givenTokens == 2) {
                    sendMessage(chatID, "Поздравляем! \uD83C\uDF8A\uD83C\uDF8A\uD83C\uDF8A \nВ нашем розыгрыше ты выиграл аж 2 QCOINа (!!), которые ты можешь потратить на доставку себе напитка из кафе командой /order.");
                    //System.out.println("Client " + newClient.getTgNickname() + " won 2 QCOINs by order! Number of his QCOINs before it: " + newClient.getNumberOfTokens());
                } else if (givenTokens == 3) {
                    sendMessage(chatID, "Поздравляем! \uD83C\uDF8A\uD83C\uDF8A\uD83C\uDF8A \nВ нашем розыгрыше ты выиграл аж 3 QCOINа \uD83E\uDD2F\uD83E\uDD2F, которые ты можешь потратить на доставку себе напитка из кафе командой /order.");
                    //System.out.println("Client " + newClient.getTgNickname() + " won 3 QCOINs by order! Number of his QCOINs before it: " + newClient.getNumberOfTokens());
                }
                newClient.setNumberOfTokens(newClient.getNumberOfTokens() + givenTokens);
                newClient.setRegistered(true);
                System.out.println("Client " + newClient.getTgNickname() + " (tracked: " + newClient.isTracked() + ") won " + givenTokens + " QCOINs by order! Number of his QCOINs after it: " + newClient.getNumberOfTokens());
            }

            if (newClient.getNumberOfTokens() < 1 && DB.getTotalNumberOfCompleteOrders() > firstOrdersForFree) {
                sendMessage(chatID, notEnoughTokens());
                return;
            }

            Order blankOrder = newClient.makeOrder();
            System.out.println("Order started, coffee choosing now. Client: " + newClient.getTgNickname() + ". Num of QCOINS: " + newClient.getNumberOfTokens());

            Main.pendingOrders.add(blankOrder);

            // Sending coffee menu
            sendMessage(chatID, "ВНИМАНИЕ! Продолжая пользоваться ботом, ты соглашаешься на прваила, описанные на фото \uD83D\uDC46" + "\n\n\uD83D\uDC8E Текущее кол-во QCOINов на балансе: " + newClient.getNumberOfTokens() + ". \nТы можешь использовать один QCOIN для доставки одного напитка. Но при этом напиток все равно покупается за твои деньги, переведенные Q-erу. Чтобы получить QCOINов, помоги кому-то с доставкой напитка командой /deliver_now. ", "client_rules.jpg");
            sendMessage(chatID, orderPrompt(), constructKeyboard(globalMenu(), blankOrder.getModifier()));

            blankOrder.setStatus(Status.COFFEE_PICKING);
        }
    }
}