package com.example.T2bleBot;

import func.TablesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.*;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final TablesService ts;
    public UpdateConsumer(@Value("${bot.token}") String botToken, TablesService ts) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.ts = ts;
    }

    @Override
    public void consume(Update update) {

        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            String username = update.getMessage().getChat().getUserName();
            ts.ensureUserExists(chatId);
            String session = ts.getSession(chatId);
            boolean isReg = ts.Is_registered(chatId);

            if ((messageText.equals("/start") && !isReg)) {
                try {
                    sendStartButton(chatId, "Приветствую, что бы начать работу с ботом вам нужно зарегистрироваться, данные для регистрации вы можете получить от начальства");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }

            } else if (messageText.equals("/start")) {
                sendMessage(chatId, "Вы уже начали работу с ботом, если нужна помощь по функционалу, то используйте команду /help");
            }else if(messageText.equals("/help")) {
                sendMessage(chatId, """
                        Если у вас проблема с работой бота, случился какой нибудь баг, или просто нужно связаться с создателем, вы можете с ним связаться по этому нику: @Ujyljy_18
                        
                        Так же можете попробовать начать работу с ботом с начала с помощью команды /restart
                        """);
            }else if(messageText.equals("/restart")) {
                ts.changeSession(chatId, "Ничего");
                ts.changeRegistered(chatId, false);
                sendMessage(chatId, "Готово! Теперь попробуйте ввести команду /start");
            }else if (session.equals("Вход в аккаунт")) {
                String[] checkLogin = messageText.split(" ");
                if (checkLogin.length == 2) {
                    String userLogin = checkLogin[0];
                    String userPassword = checkLogin[1];
                    String login = ts.login(chatId, userLogin, userPassword);
                    switch (login) {
                        case "Вы вошли в аккаунт":
                            sendMessage(chatId, login);
                            ts.changeRegistered(chatId, true);
                            ts.changeSession(chatId, "Ничего");
                            try {
                                boolean adm = ts.checkAdmin(chatId);
                                sendMenuButton(chatId, adm, "Выберите действие");
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case "Неверный пароль!!!":
                            sendMessage(chatId, login);
                            break;
                        case "Такого пользователя не существует!":
                            sendMessage(chatId, login);
                            break;

                    }
                } else {
                    sendMessage(chatId, "Неправильный формат!!!");
                }
            } else if(session.equals("Создание")) {
                String[] createLogin = messageText.split(" ");
                if (createLogin.length == 2) {
                    String newUserLogin = createLogin[0];
                    String newUserPassword = createLogin[1];
                    boolean isAdminMode = ts.getAdminSession(chatId);
                    boolean adm = ts.checkAdmin(chatId);
                    sendMessage(chatId, ts.createAccount(isAdminMode, newUserLogin, newUserPassword));
                    ts.changeSession(chatId, "Ничего");
                    try {
                        sendMenuButton(chatId, adm, "Выберите действие");
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    sendMessage(chatId, "Неправильный формат!!!");
                }
            }else if(session.equals("Увольнение")) {
                boolean adm = ts.checkAdmin(chatId);
                sendMessage(chatId, ts.deleteAccount(messageText));
                try {
                    sendMenuButton(chatId, adm, "Выберите действие");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (session.equals("Вписывает логин")) {
                boolean isHere = ts.findUserByLogin(messageText);
                if (isHere) {
                    ts.setUserSaves(chatId, messageText);
                    ts.changeSession(chatId, "Принуд выб этажа");
                    try {
                        sendFloorsButton(chatId, "Выберите этаж на котором находится желаемое место");
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    sendMessage(chatId, "Пользователь не найден");
                }
            } else if (session.equals("Вписывает логин для удаления")) {
                boolean isHere = ts.findUserByLogin(messageText);
                boolean adm = ts.checkAdmin(chatId);
                if (isHere) {

                    sendMessage(chatId, ts.clearTable(messageText));
                    ts.changeSession(chatId, "Ничего");
                    try {
                        sendMenuButton(chatId, adm, "Выберите действие :)");
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    sendMessage(chatId, "Пользователь не найден");
                }
            }
        } else if(update.hasCallbackQuery()) {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String username = update.getCallbackQuery().getFrom().getUserName();
            String data = update.getCallbackQuery().getData();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            boolean isReg = ts.Is_registered(chatId);
            boolean adm = ts.checkAdmin(chatId);
            boolean admin = false;
            if (data.equals("registration") && !isReg && ts.getSession(chatId).equals("Ничего")) {
                editMessage(chatId, messageId, "Введите логин и пароль через пробел ( EgorStepn 12435 )");
                ts.changeSession(chatId, "Вход в аккаунт");
            } else if (data.equals("registration")) {
                sendMessage(chatId, "Вы уже зарегистрированы!!!");
            } else if (data.equals("quit") && isReg && ts.getSession(chatId).equals("Ничего")) {
                ts.logout(chatId);
                editMessage(chatId, messageId, "Вы вышли из аккаунта");
                try {
                    sendStartButton(chatId, "Войдите в аккаунт");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (data.equals("role") && isReg && ts.getSession(chatId).equals("Ничего")) {
                try {
                    sendRoleButton(chatId, messageId, "Выберите роль:");
                    ts.changeSession(chatId, "Выбор роли");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if(data.equals("back") && isReg && !ts.getSession(chatId).equals("Ничего")) {
                try {
                    ts.changeSession(chatId, "Ничего");
                    sendMenuButton(chatId, messageId, adm, "Выберите действие");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if((data.equals("nonAdm") || data.equals("adm")) && isReg && ts.getSession(chatId).equals("Выбор роли")) {
                if (data.equals("adm")) {
                    ts.setAdminSession(chatId, true);  // Сохраняем в сервисе
                } else {
                    ts.setAdminSession(chatId, false);
                }
                try {
                    sendBackButton(chatId, messageId, "Для создания пользователя введите желаемый логин и пароль через пробел (DanilKolbas 165410)");
                    ts.changeSession(chatId, "Создание");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (data.equals("delete") && isReg && ts.getSession(chatId).equals("Ничего")) {
                try {
                    sendBackButton(chatId, messageId, "Введите логин пользователя для удаления");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                ts.changeSession(chatId, "Увольнение");
            }else if (data.equals("takeTable") && isReg && ts.getSession(chatId).equals("Ничего")) {
                try {
                    sendFloorsButton(chatId, messageId, "Выберите этаж на котором находится желаемое место");
                    ts.changeSession(chatId, "Выбор этажа");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }else if (data.startsWith("table_") && isReg && ts.getSession(chatId).equals("Выбор места")) {
                String tableNumber = data.substring(6);
                editMessage(chatId, messageId, ts.takeTable(tableNumber, chatId));
                ts.changeSession(chatId, "Ничего");
                try {
                    sendMenuButton(chatId, messageId, adm, "Выберите действие");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (data.startsWith("floor_") && isReg && ts.getSession(chatId).equals("Выбор этажа")) {
                String floorNumber = data.substring(6);
                try {
                    sendTablesButton(chatId, messageId, "Выберите место которое желаете занять", floorNumber);
                    ts.changeSession(chatId, "Выбор места");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }else if(data.equals("clearTable") && isReg && ts.getSession(chatId).equals("Ничего")) {
                editMessage(chatId, messageId, ts.clearTable(chatId));
                try {
                    sendMenuButton(chatId, adm, "Выберите действие");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if(data.equals("aTakeTable") && isReg && ts.getSession(chatId).equals("Ничего")) {
                ts.changeSession(chatId, "Вписывает логин");
                editMessage(chatId, messageId, "Введите логин пользователя которому хотите занять место");
            }else if (data.startsWith("table_") && isReg && ts.getSession(chatId).equals("Принуд выб места")) {
                String tableNumber = data.substring(6);
                editMessage(chatId, messageId, ts.takeTable(tableNumber, ts.getUserSaves(chatId)));
                ts.changeSession(chatId, "Ничего");
                try {
                    sendMenuButton(chatId, adm, "Выберите действие");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (data.startsWith("floor_") && isReg && ts.getSession(chatId).equals("Принуд выб этажа")) {
                String floorNumber = data.substring(6);
                try {
                    sendTablesButton(chatId, messageId, "Выберите место которое желаете занять", floorNumber);
                    ts.changeSession(chatId, "Принуд выб места");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if(data.equals("aClearTable") && isReg && ts.getSession(chatId).equals("Ничего")) {
                ts.changeSession(chatId, "Вписывает логин для удаления");
                editMessage(chatId, messageId, "Впишите логин пользователя, которого хотите снять с места");
            }else if (!isReg) {
                sendMessage(chatId, "Вы не зарегистрировались!");
            } else {
                sendMessage(chatId, "Кнопка не сработала, так как вы уже её вызывали");
            }

        }
    }
    public void sendMessage(Long chatId, String answer) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    public void editMessage(Long chatId, Integer messageId, String newText) {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(newText)
                .build();
        try {
            telegramClient.execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public void sendMenuButton(Long chatId, Integer messageId, boolean isAdmin, String answer) throws TelegramApiException {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        if (isAdmin) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Создать пользователя", "role"),
                    createBtn("Удалить пользователя", "delete")
            ));
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Принудительно занять место", "aTakeTable")
            ));
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Принудительно освободить место место", "aClearTable")
            ));
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Выйти из аккаунта❌", "quit")
            ));
        } else {
            if (ts.checkTable(chatId)) {
                keyboard.add(new InlineKeyboardRow(
                        createBtn("Поменять место", "takeTable"),
                        createBtn("Освободить место", "clearTable")
                ));
                keyboard.add(new InlineKeyboardRow(
                        createBtn("Выйти из аккаунта❌", "quit")
                ));
            } else {
                keyboard.add(new InlineKeyboardRow(
                        createBtn("Занять место", "takeTable")
                ));
                keyboard.add(new InlineKeyboardRow(
                        createBtn("Выйти из аккаунта❌", "quit")
                ));
            }
        }
        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);

    }
    public void sendMenuButton(Long chatId, boolean isAdmin, String answer) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        if (isAdmin) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Создать пользователя", "role"),
                    createBtn("Удалить пользователя", "delete")
            ));
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Принудительно занять место", "aTakeTable")
            ));
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Принудительно освободить место место", "aClearTable")
            ));
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Выйти из аккаунта❌", "quit")
            ));
        } else {
            if (ts.checkTable(chatId)) {
                keyboard.add(new InlineKeyboardRow(
                        createBtn("Поменять место", "takeTable"),
                        createBtn("Освободить место", "clearTable")
                ));
                keyboard.add(new InlineKeyboardRow(
                        createBtn("Выйти из аккаунта❌", "quit")
                ));
            } else {
                keyboard.add(new InlineKeyboardRow(
                        createBtn("Занять место", "takeTable")
                ));
                keyboard.add(new InlineKeyboardRow(
                        createBtn("Выйти из аккаунта❌", "quit")
                ));
            }
        }
        sendMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(sendMessage);

    }
    public void sendFloorsButton(Long chatId, Integer messageId, String answer) throws TelegramApiException {
        Map<Integer, ArrayList<String>> tables = ts.getAllUntakenTables();

        ArrayList<Integer> floors = new ArrayList<>(tables.keySet());
        Collections.sort(floors);
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

            for (Integer floor : floors) {
                if (!tables.get(floor).isEmpty()) {
                    currentRow.add(createBtn(floor.toString(), "floor_" + floor.toString()));
                    if (currentRow.size() == 3) {
                        keyboard.add(currentRow);
                        currentRow = new InlineKeyboardRow();
                    }
                } else {
                    answer = "Все этажи заняты!";
                }
            }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }
        keyboard.add(new InlineKeyboardRow(
                createBtn("Назад", "back")
        ));
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();
        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }
    public void sendFloorsButton(Long chatId, String answer) throws TelegramApiException {
        Map<Integer, ArrayList<String>> tables = ts.getAllUntakenTables();

        ArrayList<Integer> floors = new ArrayList<>(tables.keySet());
        Collections.sort(floors);
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (Integer floor : floors) {
            if (!tables.get(floor).isEmpty()) {
                currentRow.add(createBtn(floor.toString(), "floor_" + floor.toString()));
                if (currentRow.size() == 3) {
                    keyboard.add(currentRow);
                    currentRow = new InlineKeyboardRow();
                }
            } else {
                answer = "Все этажи заняты!";
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }
        keyboard.add(new InlineKeyboardRow(
                createBtn("Назад", "back")
        ));
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
    }
    public void sendTablesButton(Long chatId, Integer messageId, String answer, String number) throws TelegramApiException {
        Map<Integer, ArrayList<String>> all = ts.getAllUntakenTables();
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        ArrayList<String> tables = all.get(Integer.parseInt(number));
        for (String table : tables) {
            currentRow.add(createBtn(table, "table_" + table));
            if (currentRow.size() == 4) {
                keyboard.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }
        keyboard.add(new InlineKeyboardRow(
                createBtn("Назад", "back")
        ));

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }

    public void sendRoleButton(Long chatId, Integer messageId, String answer) throws TelegramApiException {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn("Администратор", "adm"),
                createBtn("Назад", "back"),
                createBtn("Работник", "nonAdm")
        ));
        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }
    public void sendBackButton(Long chatId, Integer messageId, String answer) throws TelegramApiException {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn("Назад", "back")
        ));
        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }

    public void sendStartButton(Long chatId, String answer) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                createBtn("Войти", "registration")
        ));

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
    }
    public void sendStartButton(Long chatId, Integer messageId, String answer) throws TelegramApiException {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn("Войти", "registration")
        ));

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }
    InlineKeyboardButton createBtn(String name, String data) {
        return InlineKeyboardButton.builder()
                .text(name)
                .callbackData(data)
                .build();
    }

}
