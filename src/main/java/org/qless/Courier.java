package org.qless;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.qless.Main.*;

public class Courier {
    private final long tgID;
    private String tgNickname;
    private int numberOfOrders;
    private boolean availability;
    private HashMap<String, ArrayList<ArrayList<String>>> schedule;
    private Order currentOrder;
    private double percentOfAbortedOrders;
    private int numberOfTokens;
    private boolean isTracked;
    private LocalDateTime nextDutyTime;
    private ScheduledExecutorService executionScheduler;
    private boolean permanentReceive;
    private boolean isNotificationsAllowed;
    private LocalDateTime lastSendNotification;

    public Client sameClient;
    private LocalDateTime nextDutyTimeEnd;

    public Courier(long tgID, String tgNickname, HashMap<String, ArrayList<ArrayList<String>>> schedule) {
        this.tgID = tgID;
        this.tgNickname = tgNickname;
        this.schedule = schedule;
        this.numberOfOrders = 0;
        this.availability = false;
        this.percentOfAbortedOrders = 0.0;
        this.numberOfTokens = 0;
        this.currentOrder = null;
        this.isTracked = false;
        this.isNotificationsAllowed = true;
        this.lastSendNotification = LocalDateTime.of(2025, 2, 6, 15, 30);

        this.permanentReceive = false;
        this.executionScheduler = Executors.newScheduledThreadPool(2);

        this.sameClient = findClientByID(this.tgID);
        if (this.sameClient == null){
            this.sameClient = new Client(this.tgID, this.tgNickname);
        }

        planAskingAvailability();
        planCancelingAvailability();
    }

    public Courier(long tgID, String tgNickname) {
        this.tgID = tgID;
        this.tgNickname = tgNickname;
        this.schedule = null;
        this.numberOfOrders = 0;
        this.availability = false;
        this.percentOfAbortedOrders = 0.0;
        this.numberOfTokens = 0;
        this.currentOrder = null;
        this.isTracked = false;
        this.isNotificationsAllowed = true;
        this.lastSendNotification = LocalDateTime.of(2025, 2, 6, 15, 30);

        this.permanentReceive = false;
        this.executionScheduler = null;

        this.sameClient = findClientByID(this.tgID);
        if (this.sameClient == null){
            this.sameClient = new Client(this.tgID, this.tgNickname);
        }
        this.sameClient.sameCourier = this;


    }

    public void planAskingAvailability() {
        LocalDateTime[] nextTime = getNextDutyTime();
        this.nextDutyTime = nextTime[0];
        this.nextDutyTimeEnd = nextTime[1];

        // Определяем формат вывода. Например: "2025-02-14 15:30:45"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Преобразуем LocalDateTime в строку с указанным форматом
        //System.out.println("Next duty time for courier " + this.getTgNickname() + nextDutyTime.format(formatter) + ". It ends " + nextDutyTimeEnd.format(formatter));
        //System.out.println("name: " + this.tgNickname + ", time now: " + LocalDateTime.now() + " , next duty time: " + this.nextDutyTime);
        long delay = Duration.between(LocalDateTime.now(), this.nextDutyTime).getSeconds();

        // Запускаем задачу через рассчитанную задержку
        Courier currentCourier = this;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Main.telegramBot.askCourierAvailability(currentCourier, currentCourier.nextDutyTime);
            }
        };

        if (!scheduledCouriers.contains(currentCourier.getTgID()) && (isRunning || DB.getAllAdminsTgID().contains(currentCourier.getTgID()))) {
            this.executionScheduler.schedule(task, delay, TimeUnit.SECONDS);
            scheduledCouriers.add(currentCourier.getTgID());
        }
    }

    private void planCancelingAvailability() {
        DayOfWeek dayOfWeek = DayOfWeek.from(LocalDate.now());
        String dayOfWeekKey = Integer.toString(dayOfWeek.getValue() - 1);

        Courier currentCourier = this;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                /*
                if (currentCourier.getAvailability()) {
                    System.out.println(currentCourier.getTgNickname() + " availability has been turned off automatically.");
                }
                 */
                currentCourier.setAvailability(false);
            }
        };

        if (schedule.containsKey(dayOfWeekKey)) {
            for (ArrayList<String> timeInterval : schedule.get(dayOfWeekKey)) {
                long delay = Duration.between(LocalTime.now(), LocalTime.parse(timeInterval.get(1))).getSeconds();
                this.executionScheduler.schedule(task, delay, TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Courier @%s [ID: %d]", tgNickname, tgID);
    }

    public void track() {
        isTracked = true;
        Main.DB.updateCourier(this);
    }

    public void setCurrentOrder(Order currentOrder) {
        this.currentOrder = currentOrder;
        if (isTracked) Main.DB.updateCourier(this);
    }

    public Order getCurrentOrder() {
        return currentOrder;
    }

    public long getTgID() {
        return tgID;
    }

    public boolean isNotificationOK() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusMinutes(30);
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
        if (isTracked) Main.DB.updateCourier(this);
    }

    public LocalDateTime getNextDutyTimeEnd() {
        return nextDutyTimeEnd;
    }

    public void setNextDutyTimeEnd(LocalDateTime nextDutyTimeEnd) {
        this.nextDutyTimeEnd = nextDutyTimeEnd;
    }

    public void setPermanentReceive(Boolean permanentRecieve) {
        this.permanentReceive = permanentRecieve;
        if (isTracked) Main.DB.updateCourier(this);
    }

    public Boolean getPermanentReceive() {
        return permanentReceive;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
        /*
        if (!this.availability) {
            System.out.println("Courier " + this.tgNickname + " availability turned off.");
        }
         */
        if (isTracked) Main.DB.updateCourier(this);
    }

    public void finishedOrAbortedByClientOrder(Order order) {
        if (order.getStatus().equals(Status.ABORTED_BY_CLIENT)) {
            this.availability = true;
        }
        if (order.getStatus().equals(Status.FINISHED)) {
            this.availability = true;
            numberOfOrders += 1;
        }

        if (isTracked) Main.DB.updateCourier(this);
    }


    public LocalDateTime[] getNextDutyTime() {
        // Перебираем дни начиная с текущего дня
        LocalDateTime now = LocalDateTime.now();
        int currentDayIndex = now.getDayOfWeek().getValue() - 1;
        LocalTime currentTime = now.toLocalTime().plusMinutes(1);

        for (int i = 0; i < 8; i++) {
            int dayIndex = (currentDayIndex + i) % 7; // Учитываем переход через конец недели
            ArrayList<ArrayList<String>> daySchedule = this.schedule.get(String.valueOf(dayIndex));

            if (daySchedule != null) {
                for (ArrayList<String> timeRange : daySchedule) {
                    LocalTime startTime = LocalTime.parse(timeRange.get(0));
                    LocalTime endTime = LocalTime.parse(timeRange.get(1));

                    // Если это текущий день, ищем время позже текущего момента
                    if (i == 0 && startTime.isAfter(currentTime)) {
                        LocalDateTime nextTime = now.withHour(startTime.getHour()).withMinute(startTime.getMinute()).withSecond(0);
                        LocalDateTime nextTimeEnd = now.withHour(endTime.getHour()).withMinute(endTime.getMinute()).withSecond(0);
                        LocalDateTime[] answer = {nextTime, nextTimeEnd};
                        return answer;
                    }

                    // Если это следующий день, просто берем первое доступное время
                    if (i > 0) {
                        LocalDateTime nextDateTime = now.plusDays(i).withHour(startTime.getHour()).withMinute(startTime.getMinute()).withSecond(0);
                        LocalDateTime nextTimeEnd = now.plusDays(i).withHour(endTime.getHour()).withMinute(endTime.getMinute()).withSecond(0);
                        LocalDateTime[] answer = {nextDateTime, nextTimeEnd};
                        return answer;
                    }
                }
            }
        }

        // Если нет доступного времени
        return null;
    }

    public void stopAvailabilityLater(int time){

    }

    public void setIsNotificationsAllowed(boolean isNotificationsAllowed) {
        this.isNotificationsAllowed = isNotificationsAllowed;
    }

    public boolean isNotificationsAllowed() {
        return isNotificationsAllowed;
    }

    public void abortOrder() {
//        this.currentOrder.setStatus(Status.ABORTED_BY_CLIENT);
//        this.CurrentOrder.abort();
        if (isTracked) Main.DB.updateCourier(this);
    }

    public void receiveOrder(Order order) {
        this.availability = false;
        this.currentOrder = order;

        if (isTracked) Main.DB.updateCourier(this);
    }

    public void theFreePeriodIsOver() {
        // needs to be developed. will have access to time and date, if the free period (in this.schedule) is over changes the availability to false
        this.availability = false;

        if (isTracked) Main.DB.updateCourier(this);
    }

    public int getNumberOfOrders() {
        return numberOfOrders;
    }

    public boolean isTracked() {
        return isTracked;
    }

    public void setNumberOfOrders(int numberOfOrders) {
        this.numberOfOrders = numberOfOrders;
        if (isTracked) Main.DB.updateCourier(this);
    }

    public boolean getAvailability() {
        return availability;
    }

    public void setSchedule(HashMap<String, ArrayList<ArrayList<String>>> schedule) {
        this.schedule = schedule;
        if (isTracked) Main.DB.updateCourier(this);
    }

    public HashMap<String, ArrayList<ArrayList<String>>> getSchedule() {
        return schedule;
    }

    public double getPercentOfAbortedOrders() {
        return percentOfAbortedOrders;
    }

    public void setPercentOfAbortedOrders(double percentOfAbortedOrders) {
        this.percentOfAbortedOrders = percentOfAbortedOrders;
        if (isTracked) Main.DB.updateCourier(this);
    }

    public int getNumberOfTokens() {
        this.numberOfTokens = this.sameClient.getNumberOfTokens();
        return numberOfTokens;
    }

    public void setNumberOfTokens(int numberOfTokens) {
        this.numberOfTokens = numberOfTokens;
        this.sameClient.setNumberOfTokens(this.numberOfTokens);
    }
}
