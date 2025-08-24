package org.qless;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.time.format.DateTimeFormatter;

import static java.lang.reflect.Array.set;

public class Database {
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public Database(String connectionString, String databaseName) {
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase(databaseName);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    private HashMap<String, ArrayList<ArrayList<String>>> convertDocToSchedule(Document document) {
        HashMap<String, ArrayList<ArrayList<String>>> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            ArrayList<ArrayList<String>> daySlots = new ArrayList<>();
            List<List<String>> slots = (List<List<String>>) entry.getValue();
            for (List<String> slot : slots) {
                daySlots.add(new ArrayList<>(slot));
            }
            map.put(entry.getKey(), daySlots);
        }
        return map;
    }

    private HashMap<Status, LocalDateTime> convertDocToStatusHistory(Document document) {
        HashMap<Status, LocalDateTime> statusHistory = new HashMap<>();
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Status status = Status.valueOf(entry.getKey());
            LocalDateTime dateTime = LocalDateTime.ofInstant(((Date) entry.getValue()).toInstant(), ZoneId.of("Europe/Moscow"));
            statusHistory.put(status, dateTime);
        }
        return statusHistory;
    }

    private Document convertStatusHistoryToDoc(HashMap<Status, LocalDateTime> statusHistory) {
        Document document = new Document();
        statusHistory.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    Date date = Date.from(entry.getValue().atZone(ZoneId.of("Europe/Moscow")).toInstant());
                    document.append(entry.getKey().name(), date);
                });
        return document;
    }

    private Client constructClientByDoc(Document doc) {
        Client client = new Client(doc.getLong("tgID"), doc.getString("tgNickname"));
        client.setNumberOfOrders(doc.getInteger("numberOfOrders"));
        client.setRegistered(doc.getBoolean("registered"));
        client.setNumberOfTokens(doc.getInteger("numberOfTokens"));
        client.setLastOrderDate(LocalDate.parse(doc.getString("lastOrderDate"), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        client.setNotificationsAllowed(doc.getBoolean("notificationsAllowed"));
        return client;
    }

    public void constructStatisticByDoc() {
        MongoCollection<Document> collection = database.getCollection("statistics");
        Document doc = collection.find(Filters.eq("date", LocalDate.now())).first();
        TelegramBot.num_of_starts = doc.getInteger("num_of_starts");
        TelegramBot.num_of_init_orders = doc.getInteger("num_of_init_orders");
        TelegramBot.num_of_launching_orders = doc.getInteger("num_of_launching_orders");
        TelegramBot.num_of_matched_orders = doc.getInteger("num_of_matched_orders");
        TelegramBot.num_of_completed_orders = doc.getInteger("num_of_completed_orders");
        TelegramBot.num_of_deliver_now = doc.getInteger("num_of_deliver_now");
        TelegramBot.num_of_turning_off_one_day = doc.getInteger("num_of_turning_off_one_day");
        TelegramBot.num_of_turning_off_forever = doc.getInteger("num_of_turning_off_forever");
        TelegramBot.num_of_turning_on = doc.getInteger("num_of_turning_on");
        TelegramBot.num_of_blocking = doc.getInteger("num_of_blocking");
        TelegramBot.num_of_accept_to_deliver = doc.getInteger("num_of_accept_to_deliver");
        TelegramBot.num_of_send_to_deliver = doc.getInteger("num_of_send_to_deliver");

    }

    public Integer getTotalNumberOfCompleteOrders() {
        MongoCollection<Document> collection = database.getCollection("orders");
        return (int) collection.countDocuments(Filters.or(
                Filters.eq("status", Status.FINISHED.name()),
                Filters.eq("status", Status.DELIVERED.name())
        ));
    }

    public ArrayList getAllTestersTgID() {
        MongoCollection<Document> collection = database.getCollection("testers");
        FindIterable<Document> docs = collection.find();
        ArrayList testers = new ArrayList();
        for (Document doc : docs) {
            testers.add(doc.getLong("tgID"));
        }
        return testers;
    }

    public ArrayList getAllAdminsTgID() {
        MongoCollection<Document> collection = database.getCollection("admin");
        FindIterable<Document> docs = collection.find();
        ArrayList admins = new ArrayList();
        for (Document doc : docs) {
            admins.add(doc.getLong("tgID"));
        }
        return admins;
    }

    private Courier constructCourierByDoc(Document doc) {
        Courier courier;
        if (doc.get("schedule") == null) {
            courier = new Courier(doc.getLong("tgID"), doc.getString("tgNickname"));
        } else {
            courier = new Courier(doc.getLong("tgID"), doc.getString("tgNickname"),
                    convertDocToSchedule((Document) doc.get("schedule")));
        }
        courier.setNumberOfOrders(doc.getInteger("numberOfOrders"));
        courier.setAvailability(doc.getBoolean("availability"));
        courier.setIsNotificationsAllowed(doc.getBoolean("isNotificationsAllowed"));
        try {
            courier.setPercentOfAbortedOrders(doc.getDouble("percentOfAbortedOrders"));
        } catch (Exception e) {
            courier.setPercentOfAbortedOrders(doc.getInteger("percentOfAbortedOrders"));
        }
        //courier.setNumberOfTokens(doc.getInteger("numberOfTokens"));
        return courier;
    }

    private Order constructOrderByDoc(Document doc) {
        Client client = getClientByTgID(doc.getLong("clientTgID"));
        Courier courier = getCourierByTgID(doc.getLong("courierTgID"));
        Order order = new Order(client);
        order.setCourier(courier);
        order.setCoffee(doc.getString("coffee"));
        order.setSyrup(doc.getString("syrup"));
        order.setStatus(Status.valueOf(doc.getString("status")));
        order.setNameOfClassroom(doc.getString("nameOfClassroom"));
        order.setDateTime(LocalDateTime.ofInstant(doc.getDate("dateTime").toInstant(), ZoneId.systemDefault()));
        order.setStatusHistory(convertDocToStatusHistory((Document) doc.get("statusHistory")));
        return order;
    }

    public Client getClientByTgID(long tgID) {
        MongoCollection<Document> collection = database.getCollection("clients");
        Document doc = collection.find(Filters.eq("tgID", tgID)).first();
        if (doc != null) {
            return constructClientByDoc(doc);
        }
        return null;
    }

    public Courier getCourierByTgID(long tgID) {
        MongoCollection<Document> collection = database.getCollection("couriers");
        Document doc = collection.find(Filters.eq("tgID", tgID)).first();
        if (doc != null) {
            return constructCourierByDoc(doc);
        }
        return null;
    }

    public Order getMostRecentOrder(long clientTgID, long courierTgID) {
        MongoCollection<Document> collection = database.getCollection("orders");
        Document doc = collection.find(Filters.and(
                        Filters.eq("clientTgID", clientTgID),
                        Filters.eq("courierTgID", courierTgID)))
                .sort(Sorts.descending("dateTime"))
                .first();
        if (doc != null) {
            return constructOrderByDoc(doc);
        }
        return null;
    }

    public ArrayList<Client> getAllClients() {
        MongoCollection<Document> collection = database.getCollection("clients");
        FindIterable<Document> docs = collection.find();

        ArrayList<Client> clients = new ArrayList<Client>();
        for (Document doc : docs) {
            clients.add(constructClientByDoc(doc));
        }
        return clients;
    }

    public ArrayList<Courier> getAllCouriers() {
        MongoCollection<Document> collection = database.getCollection("couriers");
        FindIterable<Document> docs = collection.find();
        ArrayList<Courier> couriers = new ArrayList<Courier>();
        for (Document doc : docs) {
            couriers.add(constructCourierByDoc(doc));
        }
        return couriers;
    }

    public ArrayList<Order> getAllOrders() {
        MongoCollection<Document> collection = database.getCollection("orders");
        FindIterable<Document> docs = collection.find();
        ArrayList<Order> orders = new ArrayList<Order>();
        for (Document doc : docs) {
            orders.add(constructOrderByDoc(doc));
        }
        return orders;
    }

    public void updateClient(Client client) {
        Client existingClient = getClientByTgID(client.getTgID());
        if (existingClient != null) {
            updateExistingClient(client);
        } else {
            saveClient(client);
        }
    }

    public void updateCourier(Courier courier) {
        Courier existingCourier = getCourierByTgID(courier.getTgID());
        if (existingCourier != null) {
            updateExistingCourier(courier);
        } else {
            saveCourier(courier);
        }
    }

    public void updateOrder(Order order) {
        Order existingOrder = getMostRecentOrder(order.getClient().getTgID(), order.getCourier().getTgID());
        if (existingOrder != null && existingOrder.getDateTime().isAfter(LocalDateTime.now().minusHours(1))) {
            updateExistingOrder(order);
        } else {
            saveOrder(order);
        }
    }

    public void updateStatistic() {
        MongoCollection<Document> collection = database.getCollection("statistics");
        Document lastDoc = collection.find().sort(new Document("_id", -1)).first();

        // Проверяем, найден ли документ
        if (lastDoc != null) {

        } else {
            // Обработка ситуации, когда коллекция пуста
        }
    }

    private void saveStatistics() {
        MongoCollection<Document> collection = database.getCollection("statistics");
        Document doc = new Document("_id", new ObjectId())
                .append("date", LocalDate.now().toString())
                .append("num_of_starts", TelegramBot.num_of_starts)
                .append("num_of_init_orders", TelegramBot.num_of_init_orders)
                .append("num_of_launching_orders", TelegramBot.num_of_launching_orders)
                .append("num_of_matched_orders", TelegramBot.num_of_matched_orders)
                .append("num_of_completed_orders", TelegramBot.num_of_completed_orders)
                .append("num_of_deliver_now", TelegramBot.num_of_deliver_now)
                .append("num_of_turning_off_one_day", TelegramBot.num_of_turning_off_one_day)
                .append("num_of_turning_off_forever", TelegramBot.num_of_turning_off_forever)
                .append("num_of_turning_on", TelegramBot.num_of_turning_on)
                .append("num_of_blocking", TelegramBot.num_of_blocking)
                .append("num_of_accept_to_deliver", TelegramBot.num_of_accept_to_deliver)
                .append("num_of_send_to_deliver", TelegramBot.num_of_send_to_deliver);
        collection.insertOne(doc);
    }

    private void saveClient(Client client) {
        MongoCollection<Document> collection = database.getCollection("clients");
        Document doc = new Document("_id", new ObjectId())
                .append("tgID", client.getTgID())
                .append("tgNickname", client.getTgNickname())
                .append("numberOfOrders", client.getNumberOfOrders())
                .append("registered", client.getRegistered())
                .append("numberOfTokens", client.getNumberOfTokens())
                .append("lastOrderDate", client.getLastOrderDate().toString())
                .append("notificationsAllowed", client.getNotificationsAllowed());
        collection.insertOne(doc);
    }

    private void updateExistingClient(Client client) {
        MongoCollection<Document> collection = database.getCollection("clients");
        Document updatedDoc = new Document()
                .append("tgNickname", client.getTgNickname())
                .append("numberOfOrders", client.getNumberOfOrders())
                .append("registered", client.getRegistered())
                .append("numberOfTokens", client.getNumberOfTokens())
                .append("lastOrderDate", client.getLastOrderDate().toString())
                .append("notificationsAllowed", client.getNotificationsAllowed());
        collection.updateOne(Filters.eq("tgID", client.getTgID()), new Document("$set", updatedDoc));
    }

    private void updateStatistics() {
        MongoCollection<Document> collection = database.getCollection("statistics");
        Document doc = new Document("_id", new ObjectId())
                .append("date", LocalDate.now().toString())
                .append("num_of_starts", TelegramBot.num_of_starts)
                .append("num_of_init_orders", TelegramBot.num_of_init_orders)
                .append("num_of_launching_orders", TelegramBot.num_of_launching_orders)
                .append("num_of_matched_orders", TelegramBot.num_of_matched_orders)
                .append("num_of_completed_orders", TelegramBot.num_of_completed_orders)
                .append("num_of_deliver_now", TelegramBot.num_of_deliver_now)
                .append("num_of_turning_off_one_day", TelegramBot.num_of_turning_off_one_day)
                .append("num_of_turning_off_forever", TelegramBot.num_of_turning_off_forever)
                .append("num_of_turning_on", TelegramBot.num_of_turning_on)
                .append("num_of_blocking", TelegramBot.num_of_blocking)
                .append("num_of_accept_to_deliver", TelegramBot.num_of_accept_to_deliver)
                .append("num_of_send_to_deliver", TelegramBot.num_of_send_to_deliver);
        collection.updateOne(Filters.eq("date", LocalDate.now()), new Document("$set", doc));
    }

    public void addCourierToDB(Courier courier) {
        saveCourier(courier);
        courier.track();
        //System.out.println("TRACKED! Courier " + courier.getTgNickname() + " added to the database in DB. Num of QCOINS: " + courier.getNumberOfTokens());
    }

    private void saveCourier(Courier courier) {
        MongoCollection<Document> collection = database.getCollection("couriers");
        Document doc = new Document("_id", new ObjectId())
                .append("tgID", courier.getTgID())
                .append("tgNickname", courier.getTgNickname())
                .append("numberOfOrders", courier.getNumberOfOrders())
                .append("availability", courier.getAvailability())
                .append("schedule", courier.getSchedule())
                .append("percentOfAbortedOrders", courier.getPercentOfAbortedOrders())
                .append("numberOfTokens", courier.getNumberOfTokens())
                .append("isNotificationsAllowed", courier.isNotificationsAllowed());
        collection.insertOne(doc);
    }

    public void updateExistingCourier(Courier courier) {
        MongoCollection<Document> collection = database.getCollection("couriers");
        Document updatedDoc = new Document()
                .append("tgNickname", courier.getTgNickname())
                .append("numberOfOrders", courier.getNumberOfOrders())
                .append("availability", courier.getAvailability())
                .append("schedule", courier.getSchedule())
                .append("percentOfAbortedOrders", courier.getPercentOfAbortedOrders())
                .append("numberOfTokens", courier.getNumberOfTokens())
                .append("isNotificationsAllowed", courier.isNotificationsAllowed());
        collection.updateOne(Filters.eq("tgID", courier.getTgID()), new Document("$set", updatedDoc));
    }

    private void saveOrder(Order order) {
        MongoCollection<Document> collection = database.getCollection("orders");
        Document doc = new Document("_id", new ObjectId())
                .append("clientTgID", order.getClient().getTgID())
                .append("courierTgID", order.getCourier().getTgID())
                .append("coffee", order.getCoffee())
                .append("syrup", order.getSyrup())
                .append("status", order.getStatus().name())
                .append("nameOfClassroom", order.getNameOfClassroom())
                .append("dateTime", Date.from(order.getDateTime().atZone(ZoneId.systemDefault()).toInstant()))
                .append("statusHistory", convertStatusHistoryToDoc(order.getStatusHistory()));
        collection.insertOne(doc);
    }

    private void updateExistingOrder(Order order) {
        MongoCollection<Document> collection = database.getCollection("orders");
        Order existingOrder = getMostRecentOrder(order.getClient().getTgID(), order.getCourier().getTgID());

        Document updatedDoc = new Document()
                .append("clientTgID", order.getClient().getTgID())
                .append("courierTgID", order.getCourier().getTgID())
                .append("coffee", order.getCoffee())
                .append("syrup", order.getSyrup())
                .append("status", order.getStatus().name())
                .append("nameOfClassroom", order.getNameOfClassroom())
                .append("dateTime", Date.from(order.getDateTime().atZone(ZoneId.systemDefault()).toInstant()))
                .append("statusHistory", convertStatusHistoryToDoc(order.getStatusHistory()));
        collection.updateOne(Filters.and(
                Filters.eq("clientTgID", order.getClient().getTgID()),
                Filters.eq("courierTgID", order.getCourier().getTgID()),
                Filters.eq("dateTime", Date.from(existingOrder.getDateTime().atZone(ZoneId.systemDefault()).toInstant()))
        ), new Document("$set", updatedDoc));
    }

    public void close() {
        mongoClient.close();
    }
}