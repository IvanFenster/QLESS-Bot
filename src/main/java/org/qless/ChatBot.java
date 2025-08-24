package org.qless;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatBot {
    public String greeting() {
        String text = "Привет! С помощью сервиса QLESS ты сможешь заказать себе доставку напитка из школьного кафе в любое время. Вся система работает на взаимопомощи, доставил напиток - получил возможность заказать самому \uD83E\uDEF6\n\n\uD83E\uDDCB Чтобы заказать напиток введи команду /order \n\uD83D\uDECD Чтобы доставить напиток введи команду /deliver_now \n\n Подписывайся на наш канал, чтобы ничего не пропустить " + Config.getProjectUpdates();
        return text;
    }

    public String orderPrompt() {
        return "☕\uFE0F\uD83E\uDD64\uD83E\uDDC3\n\nВыбери, что ты хочешь заказать:";

    }

    public String syrupPrompt() {
        return "Выбери сироп, который ты хочешь добавить к своему напитку (если он есть в наличии). Сироп добавляет к стоимости напитка 35 руб.";
    }

    public String notApproriateTimeRefuse() {
        return "\uD83D\uDE48 Кафе закрыто или близко к этому. Попробуй сделать /order, в период с 9:00 до 19:30. (в Субботу с 10:00)";
    }

    public String orderConfirmation(String coffee, String syrup) {
        return String.format("Ты заказал %s с сиропом \"%s\"!", coffee, syrup);
    }

    public String supportMessage() {
        return "\uD83D\uDE46\u200D♀\uFE0F При возникновении проблем с доставкой или с взаимодействием Q-erом пиши сюда: " + Config.getDeliverySupport() + "\n\uD83D\uDCBB При возникновении проблем с работой бота, пиши сюда: " + Config.getTechSupport();

    }

    public String notEnoughTokens() {
        return "Ты не можешь сделать заказ, так как у тебя нет QCOINов на балансе.\n\n\uD83D\uDC8E Чтобы получить QCOINы, доставь напиток через наш сервис. Ты можешь сделать это прямо сейчас или когда у тебя будет время командой /deliver_now.";
    }

    public String noCourierFound() {
        return "К сожалению, никто из Q-erов не смог принять твой заказ. Попробуй сделать /order чуть позже.";
    }

    public String notWorking() {
        return "Бот сейчас не работает, но мы уже скоро вернемся! Следи за обновлениями в нашем канале " + Config.getProjectUpdates();
    }

    public ArrayList<ArrayList<String>> globalMenu() {
        return new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Кофе и матча \uD83E\uDDCB")),
                new ArrayList<>(List.of("Пакетированный чай \uD83E\uDED6")),
                new ArrayList<>(List.of("Другое"))
        ));
    }

    public static ArrayList<ArrayList<String>> teaMenu() {
        return new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("♣️ Earl Grey", "\uD83E\uDDCA♣️ Earl Grey")),
                new ArrayList<>(Arrays.asList("♣️ English Breakfast", "\uD83E\uDDCA♣️ English Breakfast")),
                new ArrayList<>(Arrays.asList("🍃 Milk Oolong", "\uD83E\uDDCA🍃 Milk Oolong")),
                new ArrayList<>(Arrays.asList("🍃 Oriential Bloom", "\uD83E\uDDCA🍃 Oriential Bloom")),
                new ArrayList<>(Arrays.asList("🍃 Green Fusion", "\uD83E\uDDCA🍃 Green Fusion")),
                new ArrayList<>(Arrays.asList("🍇 Strawberry Desert", "\uD83E\uDDCA🍇 Strawberry Desert")),
                new ArrayList<>(Arrays.asList("🍇 Rooibush Orange", "\uD83E\uDDCA🍇 Rooibush Orange")),
                new ArrayList<>(Arrays.asList("Назад ↩\uFE0F"))
        ));
    }

    public ArrayList<ArrayList<String>> CAPSteaMenu() {
        return new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("♣️ EARL GREY", "\uD83E\uDDCA♣️ EARL GREY")),
                new ArrayList<>(Arrays.asList("🍃 MILK OOLONG", "\uD83E\uDDCA🍃 MILK OOLONG")),
                new ArrayList<>(Arrays.asList("🍃 ORIENTIAL BLOOM", "\uD83E\uDDCA🍃 ORIENTIAL BLOOM")),
                new ArrayList<>(Arrays.asList("🍇 STRAWBERRY DESERT", "\uD83E\uDDCA🍇 STRAWBERRY DESERT")),
                new ArrayList<>(Arrays.asList("🍇 ROOIBUSH ORANGE", "\uD83E\uDDCA🍇 ROOIBUSH ORANGE")),
                new ArrayList<>(Arrays.asList("Назад ↩\uFE0F"))
        ));
    }

    public ArrayList<ArrayList<String>> coffeeMenu() {
        return new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("Эспрессо: 80 р.", "Двойной эспрессо: 160 р.")),
                new ArrayList<>(Arrays.asList("Американо: 85 р.", "Айс американо: 85 р.")),
                new ArrayList<>(Arrays.asList("Латте: 150 р.", "Айс латте: 165 р.")),
                new ArrayList<>(Arrays.asList("Капучино: 120 р.", "Айс капучино: 120 р.")),
                new ArrayList<>(Arrays.asList("Большой капучино: \n165 р.", "Большой айс капучино: \n165 р.")),
                new ArrayList<>(Arrays.asList("Раф: 170 р.", "Айс раф: 170 р.")),
                new ArrayList<>(Arrays.asList("Флэт-уайт: 125 р.", "Айс флэт-уайт: 125 р.")),
                new ArrayList<>(Arrays.asList("Горячий шоколад: 185 р.", "Какао: 100 р.")),
                new ArrayList<>(Arrays.asList("Матча: 180 р.", "Айс матча: 180 р.")),
                new ArrayList<>(Arrays.asList("Назад ↩\uFE0F"))
        ));
    }

    public ArrayList<ArrayList<String>> otherMenu() {
        return new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("Горячий шоколад: 185 р.")),
                new ArrayList<>(Arrays.asList("Какао: 100 р.")),
                new ArrayList<>(Arrays.asList("Назад ↩\uFE0F"))
        ));
    }

    ArrayList<String> menuOptions() {
        ArrayList<String> options = new ArrayList<>();
        for (ArrayList<String> coffeeList : coffeeMenu()) {
            if (!coffeeList.equals(Arrays.asList("Назад ↩\uFE0F"))) {
                options.addAll(coffeeList);
            }
        }
        for (ArrayList<String> teaList : teaMenu()) {
            if (!teaList.equals(Arrays.asList("Назад ↩\uFE0F"))) {
                options.addAll(teaList);
            }
        }
        for (ArrayList<String> otherList : otherMenu()) {
            if (!otherList.equals(Arrays.asList("Назад ↩\uFE0F"))) {
                options.addAll(otherList);
            }
        }
        return options;
    }

    static ArrayList<String> teaOptionsOnly() {
        ArrayList<String> options = new ArrayList<>();

        for (ArrayList<String> teaList : teaMenu()) {
            if (!teaList.equals(Arrays.asList("Назад ↩\uFE0F"))) {
                options.addAll(teaList);
            }
        }

        return options;
    }

    public ArrayList<ArrayList<String>> syrupMenu() {
        return new ArrayList<>(Arrays.asList(
                new ArrayList<>(List.of("Без сиропа")),
                new ArrayList<>(Arrays.asList("Карамель", "Соленая карамель")),
                new ArrayList<>(Arrays.asList("Ваниль", "Айриш")),
                new ArrayList<>(Arrays.asList("Шоколад", "Шоколадное печенье")),
                new ArrayList<>(Arrays.asList("Клён", "Мята")),
                new ArrayList<>(Arrays.asList("Банан", "Кокос")),
                new ArrayList<>(Arrays.asList("Клубника", "Малина")),
                new ArrayList<>(Arrays.asList("Дикие ягоды", "Лаванда"))
        ));
    }

    ArrayList<String> syrupOptions() {
        ArrayList<String> options = new ArrayList<>();
        for (ArrayList<String> syrupList : syrupMenu()) {
            options.addAll(syrupList);
        }
        return options;
    }
}
