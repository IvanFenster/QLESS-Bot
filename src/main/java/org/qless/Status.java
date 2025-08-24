package org.qless;

public enum Status {
    INITIALISED,            // Инициализирован
    DECLINED,                // Отклонен

    COFFEE_PICKING,
    SYRUP_PICKING,
    PROVIDING_CLASSROOM,
    CONFIRMING_CLASSROOM,
    LEAVING_COMMENT,

    COURIER_REQUESTED,       // Курьер запрошен
    COURIER_NOT_FOUND,       // Курьер не найден
    CONFIRMATION,            // Подтверждение
    BUYING,                 // Покупка
    DELIVERY,               // Доставка
    DELIVERED,              // Доставлено
    FINISHED,               // Завершен
    ABORTED_BY_CLIENT,      // Отменен клиентом
    ABORTED_BY_COURIER,     // Отменен курьером

    CANCELLING_BY_COURIER_CONFIRMATION,         // Запрос отмены от курьера на момент Подтверждение
    CANCELLING_BY_CLIENT_COFFEE_PICKING,        // Запрос отмены от клиента на момент Выбор кофе
    CANCELLING_BY_CLIENT_SYRUP_PICKING,         // Запрос отмены от клиента на момент Выбор сиропа
    CANCELLING_BY_CLIENT_CONFIRMATION,          // Запрос отмены от клиента на момент Подтверждение
    CANCELLING_BY_CLIENT_COURIER_REQUESTED,     // Запрос отмены от клиента на момент Курьер запрошен
    CANCELLING_BY_CLIENT_PROVIDING_CLASSROOM;   // Запрос отмены от клиента на момент Выбора кабинета
}