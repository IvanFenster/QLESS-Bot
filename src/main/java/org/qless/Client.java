package org.qless;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.time.LocalDate;

import static org.qless.Main.*;

public class Client {
    private final long tgID;
    private String tgNickname;
    private int numberOfOrders;
    private boolean registered;
    private int numberOfTokens;
    private Order currentOrder;
    private ArrayList<Order> allOrders;
    private boolean isTracked;
    private LocalDate lastOrderDate;
    private boolean notificationsAllowed;
    private LocalDateTime lastSendNotification;
    private boolean blockedTheBot;

    public Courier sameCourier;


    public Client(long tgID, String tgNickname) {
        this.tgID = tgID;
        this.tgNickname = tgNickname;
        this.numberOfOrders = 0;
        this.registered = false;
        this.numberOfTokens = 0;
        this.currentOrder = null;
        this.allOrders = new ArrayList<>();
        this.isTracked = false;
        this.lastOrderDate = LocalDate.of(2025, 1, 30);
        this.notificationsAllowed = true;
        this.lastSendNotification = LocalDateTime.of(2025, 2, 6, 15, 30);
        this.blockedTheBot = false;
    }

    @Override
    public String toString() {
        return String.format("Client @%s [ID: %d]", tgNickname, tgID);
    }

    public void track() {
        this.isTracked = true;
        Main.DB.updateClient(this);
    }

    public Order makeOrder() {
        // запускается из бота
        Order order = new Order(this);
        this.currentOrder = order;
        this.allOrders.add(order);
        if (isTracked) Main.DB.updateClient(this);
        return order;
    }

    public void abortOrder() {
//        this.currentOrder.setStatus(Status.ABORTED_BY_CLIENT);
        if (isTracked) Main.DB.updateClient(this);
    }

    public boolean isNotificationOK() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusMinutes(60);
        return lastSendNotification.isBefore(oneHourAgo);
    }

    public void setLastSendNotification(LocalDateTime lastSendNotification) {
        this.lastSendNotification = lastSendNotification;
    }

    public String getTgNickname() {

        return tgNickname;
    }

    public void setTgNickname(String tgNickname) {
        this.tgNickname = tgNickname;
        if (isTracked) Main.DB.updateClient(this);
    }

    public long getTgID() {
        return tgID;
    }

    public boolean isTracked() {
        return isTracked;
    }

    public Object getCurrentOrder() {
        return currentOrder;
    }

    public void confirmMoneyTransferred() {
        this.currentOrder.setStatus(Status.BUYING);
        if (isTracked) Main.DB.updateClient(this);
    }

    public void confirmCoffeeIsReceived() {
        this.currentOrder.setStatus(Status.FINISHED);
        if (isTracked) Main.DB.updateClient(this);
    }

    public int getNumberOfOrders() {
        return numberOfOrders;
    }

    public void setNumberOfOrders(int numberOfOrders) {
        this.numberOfOrders = numberOfOrders;
        if (isTracked) Main.DB.updateClient(this);
    }

    public void setBlockedTheBot(boolean blockedTheBot) {
        this.blockedTheBot = blockedTheBot;
    }

    public boolean getBlockedTheBot() {
        return blockedTheBot;
    }

    public boolean getRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
        if (isTracked) Main.DB.updateClient(this);
    }

    public void setNotificationsAllowed(boolean notificationsAllowed) {
        this.notificationsAllowed = notificationsAllowed;
        if (isTracked) Main.DB.updateClient(this);
    }

    public boolean getNotificationsAllowed() {
        return notificationsAllowed;
    }

    public int getNumberOfTokens() {
        return numberOfTokens;
    }

    public void setLastOrderDate(LocalDate lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
        if (isTracked) Main.DB.updateClient(this);
    }

    public LocalDate getLastOrderDate() {
        return lastOrderDate;
    }

    public void setNumberOfTokens(int numberOfTokens) {
        this.numberOfTokens = numberOfTokens;
        if (isTracked) Main.DB.updateClient(this);
    }
}

