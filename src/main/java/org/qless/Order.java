package org.qless;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.concurrent.TimeUnit;

import static org.qless.Main.acceptedTime;

import static org.qless.Main.scheduledCouriers;

import static org.qless.Main.telegramBot;

public class Order {
    private final Client client;
    private Courier courier;
    private java.time.LocalDateTime dateTime;
    private Status status;
    private String nameOfClassroom;
    private boolean isTracked = false;
    private HashMap<Status, LocalDateTime> statusHistory = new HashMap<>();
    private int declined;
    private int finalPrice;

    private int numOfAvailableCouriers;
    private boolean isForFree = false;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // TODO: products Enum
    private String coffee;
    private String syrup;

    public Order(Client client) {
        this.client = client;
        status = Status.INITIALISED;
        this.dateTime = java.time.LocalDateTime.now();

        if (Main.availableCouriers().isEmpty()) {
            this.status = Status.COURIER_NOT_FOUND;
//            tgBot.courierDecline(this);
        }
    }

    public void track() {
        isTracked = true;
        Main.DB.updateOrder(this);
    }

    public Boolean getIsForFree() {
        return isForFree;
    }

    public void setIsForFree(Boolean isForFree) {
        this.isForFree = isForFree;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public int getNumOfAvailableCouriers() {
        return numOfAvailableCouriers;
    }

    public void setNumOfAvailableCouriers(int numOfAvailableCouriers) {
        this.numOfAvailableCouriers = numOfAvailableCouriers;
    }

    public boolean isFinished(){
        if (this.status.equals(Status.FINISHED) || this.status.equals(Status.COURIER_NOT_FOUND) || this.status.equals(Status.ABORTED_BY_CLIENT) || this.status.equals(Status.ABORTED_BY_COURIER)){
            return true;
        } else {
            return false;
        }
    }

    public Status getStatus() {
        return status;
    }

    public String getNameOfClassroom() {
        return nameOfClassroom;
    }

    public void setNameOfClassroom(String nameOfClassroom) {
        this.nameOfClassroom = nameOfClassroom;
        if (isTracked) Main.DB.updateOrder(this);
    }

    public String getCoffee() {
        return coffee;
    }

    public String getSyrup() {
        return syrup;
    }

    public Courier getCourier() {
        return courier;
    }

    public Client getClient() {
        return client;
    }

    public void setCoffee(String coffee) {
        this.coffee = coffee;
        if (isTracked) Main.DB.updateOrder(this);
    }

    public void setSyrup(String syrup) {
        this.syrup = syrup;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.statusHistory.put(status, LocalDateTime.now());

        if (status == Status.FINISHED) {
            this.client.setNumberOfOrders(this.client.getNumberOfOrders() + 1);
            this.courier.setNumberOfOrders(this.courier.getNumberOfOrders() + 1);
        }

        if (isTracked) Main.DB.updateOrder(this);
    }

    public void setCourier(Courier courier) {
        this.courier = courier;
        if (isTracked) Main.DB.updateOrder(this);
    }

    public String getCoffeeName() {
        String coffee_name = "";
        if (this.getCoffee().startsWith("\uD83E\uDDCA")) {
            coffee_name += "Холодный чай";
            coffee_name += this.getCoffee().substring(3);
        } else if (ChatBot.teaOptionsOnly().contains(this.getCoffee())) {
            coffee_name += "Горячий чай";
            coffee_name += this.getCoffee().substring(2);
        } else {
            coffee_name += this.getCoffee().split(":")[0];
        }
        return coffee_name;
    }

    public int getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(int finalPrice) {
        this.finalPrice = finalPrice;
    }

    public int getModifier(){
        return Main.pendingOrders.indexOf(this);
    }

    public void requestCourier() {
        for (Object courier : Main.availableCouriers()) {
//            courier.askAvailability();
        }
        this.setStatus(Status.COURIER_REQUESTED);
    }

    public void courierFound(Courier courier) {
        this.courier = courier;
//        tgBot.giveContacts(this);
        this.setStatus(Status.CONFIRMATION);
    }

    public void abort() {
//        tgBot.confirmAbort(this);
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

    public HashMap<Status, LocalDateTime> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(HashMap<Status, LocalDateTime> statusHistory) {
        this.statusHistory = statusHistory;
        if (isTracked) Main.DB.updateOrder(this);
    }

    public int getDeclined() {
        return declined;
    }

    public void setDeclined(int declined) {
        this.declined = declined;
    }
}
