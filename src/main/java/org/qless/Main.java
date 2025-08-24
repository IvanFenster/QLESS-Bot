package org.qless;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    static ArrayList<Client> allClients = new ArrayList<>();
    static ArrayList<Courier> allCouriers = new ArrayList<>();
    static ArrayList<Order> pendingOrders = new ArrayList<>();
    static ArrayList<Long> classroomRespondWaiting = new ArrayList<>();
    static TelegramBot telegramBot;
    static ArrayList<ArrayList<ArrayList<LocalTime>>> acceptedTime = new ArrayList<>();
    static int percentOfIncome = Config.getPercentOfIncome();
    static int firstOrdersForFree = Config.getFirstOrdersForFree();
    static final Set<Long> scheduledCouriers = new HashSet<>();

    static boolean isTimeLimiting = true;
    static boolean isRunning = false;
    static boolean isTesting = false;
    static boolean isUsingBreaks = false;
    static boolean isOneMoreTokenForFirstOrder = Config.isOneMoreTokenForFirstOrder();

    static Database DB = new Database(System.getenv("MONGO_CONN_STRING"), Config.getDatabaseName());



    public static ArrayList<Courier> availableCouriers() {
        ArrayList<Courier> container = new ArrayList<>();
        for (Courier courier : allCouriers) {
            if (courier.getAvailability()) {
                container.add(courier);
            }
        }
        return container;
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Started");
        String botToken = System.getenv("BOT_TOKEN");
        telegramBot = new TelegramBot(botToken);

        parseJSONFile();
        loadDeliveryAgents();

        //DB.constructStatisticByDoc();



//        demoDeliveryAgents();

        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, telegramBot);
            System.out.println("TelegramBot successfully started!");
            Thread.currentThread().join();
        } catch (Exception e) {
            System.out.println("Error while starting the bot: " + e.getMessage());
            e.printStackTrace();
        }


    }

    public static void parseJSONFile() {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream jsonFile = null;

        try {
            if (isRunningFromJar()) {
                jsonFile = Main.class.getResourceAsStream("/AcceptedTime.json");
            } else {
                jsonFile = new FileInputStream("src/main/resources/AcceptedTime.json");
            }

            if (jsonFile == null) {
                throw new FileNotFoundException("AcceptedTime.json file not found");
            }

            JsonNode rootNode = objectMapper.readTree(jsonFile);

            for (int i = 0; i < 7; i++) {
                JsonNode dayNode = rootNode.path(String.valueOf(i));
                acceptedTime.add(new ArrayList<>());
                if (dayNode.isArray()) {
                    for (JsonNode timePeriod : dayNode) {
                        String startString = timePeriod.get(0).asText();
                        String[] startList = startString.split(":");

                        String endString = timePeriod.get(1).asText();
                        String[] endList = endString.split(":");

                        LocalTime startTime = LocalTime.of(Integer.parseInt(startList[0]), Integer.parseInt(startList[1]));
                        LocalTime endTime = LocalTime.of(Integer.parseInt(endList[0]), Integer.parseInt(endList[1]));

                        ArrayList<LocalTime> startAndEnd = new ArrayList<>();
                        startAndEnd.add(startTime);
                        startAndEnd.add(endTime);
                        acceptedTime.get(i).add(new ArrayList<>(startAndEnd));
                    }
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("JSON file not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
        } finally {
            if (jsonFile != null) {
                try {
                    jsonFile.close();
                } catch (IOException e) {
                    System.err.println("Error closing JSON file: " + e.getMessage());
                }
            }
        }
    }


    public static void loadDeliveryAgents() {
        allClients.addAll(DB.getAllClients());
        allCouriers.addAll(DB.getAllCouriers());

        System.out.println("Loaded " + allCouriers.size() + " couriers");

        for (Courier courier : allCouriers) {
            courier.track();
            //System.out.println("TRACKED! Courier " + courier.getTgNickname() + " loaded from DB in Main. Num of QCOINS: " + courier.getNumberOfTokens());
        }
        for (Client client : allClients) {
            client.track();
            //System.out.println("TRACKED! Courier " + client.getTgNickname() + " loaded from DB in Main. Num of QCOINS: " + client.getNumberOfTokens());

        }
    }

    public static boolean isBreakNow() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Читаем JSON в структуру: ключ - день недели в виде строки, значение - список пар строк (начало, конец)
            Map<String, List<List<String>>> rawData = mapper.readValue(
                    new File("src/main/resources/BreaksTime.json"),
                    new TypeReference<Map<String, List<List<String>>>>() {}
            );

            // Определяем текущий день недели: java.time.DayOfWeek (понедельник = 1, ..., воскресенье = 7)
            // Приводим к ключу от "0" до "6" (0 = понедельник)
            int dayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;
            String dayKey = String.valueOf(dayIndex);

            // Получаем список диапазонов для текущего дня
            List<List<String>> dayRanges = rawData.getOrDefault(dayKey, Collections.emptyList());

            LocalTime now = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            // Проходим по каждому диапазону времени
            for (List<String> timePair : dayRanges) {
                if (timePair.size() == 2) {
                    LocalTime start = LocalTime.parse(timePair.get(0), formatter);
                    LocalTime end = LocalTime.parse(timePair.get(1), formatter);
                    // Если текущее время не раньше начала и раньше конца диапазона
                    if (!now.isBefore(start) && now.isBefore(end)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }



    private static void demoDeliveryAgents() {

        HashMap<String, ArrayList<ArrayList<String>>> mockSchedule = new HashMap<>() {{
            put("0", new ArrayList<>(List.of(new ArrayList<>(List.of("10:00", "11:00")))));
        }};

        Client demoClient = new Client(123456789, "test");
        Courier demoCourier = new Courier(1920379812, "kglebaa", mockSchedule);

        demoClient.track();
        demoCourier.track();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        demoClient.setTgNickname("durov");
        demoClient.makeOrder();
        demoClient.confirmCoffeeIsReceived();

        demoCourier.setAvailability(!demoCourier.getAvailability());
        demoCourier.setTgNickname("meow");
        demoCourier.setNumberOfTokens(demoCourier.getNumberOfTokens() + 1);
    }

    public static Courier findCourierByID(long tgID) {
        for (int courierID = 0; courierID < allCouriers.size(); courierID++) {
            if (allCouriers.get(courierID).getTgID() == tgID) {
                return allCouriers.get(courierID);
            }
        }
        return null;
    }

    public static Client findClientByID(long tgID) {
        /*allClients.clear();
        allClients.addAll(DB.getAllClients());

         */
        for (int clientID = 0; clientID < allClients.size(); clientID++) {
            if (allClients.get(clientID).getTgID() == tgID) {
                return allClients.get(clientID);
            }
        }
        return null;
    }

    public static Order findOrderByCourierOrClient(Courier courier, Client client, boolean current) {
        for (Order orders : pendingOrders) {
            if (orders.getCourier() != null && courier != null) {
                if (orders.getCourier().getTgID() == courier.getTgID() && (!orders.isFinished() || !current)) {
                    return orders;
                }
            }
            if (orders.getClient() != null && client != null) {
                if (orders.getClient().getTgID() == client.getTgID() && (!orders.isFinished() || !current)) {
                    return orders;
                }
            }
        }
        return null;
    }


    protected static boolean isRunningFromJar() {
        URL resource = Main.class.getResource(Main.class.getSimpleName() + ".class");
        return resource != null && resource.getProtocol().equals("jar");
    }

    public static String escapeMarkdownV2(String text) {
        return text.replaceAll("([_\\*\\[\\]\\(\\)~`>#+\\-=|{}.!])", "\\\\$1");
    }

}