package func;
import db.Session;
import db.Tables;
import db.Users;
import jakarta.persistence.*;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatSubscriptionInviteLink;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TablesService {
    private final Map<Long, Boolean> adminSessions = new ConcurrentHashMap<>();
    private final Map<Long, String> loginSaves = new ConcurrentHashMap<>();
    @PersistenceContext
    private EntityManager entityManager;
    @Transactional
    public String createAccount(boolean is_admin, String login, String password) {
        try {
            TypedQuery<Session> query = entityManager.createQuery("SELECT s FROM UserSession s WHERE s.name = 'Ничего'", Session.class);
            List<Session> sessions = query.getResultList();
            Session session = sessions.getFirst();
            Users user = new Users();
            user.setLogin(login);
            user.setIs_admin(is_admin);
            user.setPassword(password);
            user.setCreated(LocalDate.now());
            user.setAction(session);
            user.setIs_registered(false);
            entityManager.persist(user);
            entityManager.flush();
            return "Пользователь добавлен✔";
        } catch (Exception e) {
            return "Пользователь уже существует!";
        }
    }
    @Transactional
    public String deleteAccount(String login) {
        TypedQuery<Users> query = entityManager.createQuery("SELECT u from User u WHERE login = :login", Users.class).setParameter("login", login);
        List<Users> users = query.getResultList();
            if(users != null) {
                Users user = users.getFirst();
                Tables tables = user.getTables();
                if (tables != null) {
                    tables.setUsers(null);
                    tables.setIs_taken(false);
                    entityManager.persist(tables);
                }
                entityManager.remove(user);



                return "Пользователь успешно удалён";
            } else {
                return "Такого пользователя не существует!";
            }

    }
    @Transactional
    public String login(Long chatId, String login, String password) {
        TypedQuery<Users> query = entityManager.createQuery(
                        "SELECT u FROM User u WHERE u.login = :login", Users.class)
                .setParameter("login", login);
        List<Users> users = query.getResultList();

        if (users.isEmpty()) {
            return "Такого пользователя не существует!";
        }

        Users user = users.getFirst();

        if (!password.equals(user.getPassword())) {
            return "Неверный пароль!!!";
        }

        if (user.getChatId() != null && !user.getChatId().equals(chatId) && user.isIs_registered()) {
            return "Этот аккаунт уже используется на другом устройстве!";
        }

        Users existingUser = findUserByChatId(chatId);

        if (existingUser != null) {
            if (!existingUser.getLogin().equals(login) && existingUser.isIs_registered()) {
                return "Этот чат уже привязан к другому пользователю! Выйдите из аккаунта.";
            }
            entityManager.remove(existingUser);
            entityManager.flush();

        }
        user.setChatId(chatId);
        user.setIs_registered(true);
        entityManager.merge(user);
        entityManager.flush();

        return "Вы вошли в аккаунт";
    }
    @Transactional
    public void logout(Long chatId) {
        Users user = findUserByChatId(chatId);
        user.setIs_registered(false);
        entityManager.persist(user);
        entityManager.flush();
    }
    @Transactional
    public String takeTable(String number, Long chatId) {
        TypedQuery<Tables> query = entityManager.createQuery("SELECT t FROM UserTables t WHERE number = :number", Tables.class).setParameter("number", number);
        List<Tables> tables = query.getResultList();
        Tables table = tables.getFirst();

        if (tables.isEmpty()) {
            return "Такого стола не существует!";
        } else {
            Users user = findUserByChatId(chatId);
            if (user.getTables() == null) {
                user.setTables(table);
                table.setIs_taken(true);
                entityManager.persist(user);
                entityManager.persist(table);
                entityManager.flush();
                return "Вы успешно заняли место✔";
            } else {
                Tables tables1 = user.getTables();
                tables1.setIs_taken(false);
                tables1.setUsers(null);
                entityManager.persist(tables1);
                user.setTables(table);
                table.setIs_taken(true);
                entityManager.persist(user);
                entityManager.persist(table);
                entityManager.flush();
                return "Вы успешло поменяли своё место✔";
            }
        }
    }
    @Transactional
    public String takeTable(String number, String login) {
        TypedQuery<Tables> query = entityManager.createQuery("SELECT t FROM UserTables t WHERE number = :number", Tables.class).setParameter("number", number);
        List<Tables> tables = query.getResultList();


        if (tables.isEmpty()) {
            return "Такого стола не существует!";
        } else {
            Tables table = tables.getFirst();
            TypedQuery<Users> query1 = entityManager.createQuery("SELECT u from User u WHERE login = :login", Users.class).setParameter("login", login);
            List<Users> users = query1.getResultList();
            Users user = users.getFirst();
            if (user.getTables() == null) {
                user.setTables(table);
                table.setIs_taken(true);
                entityManager.persist(user);
                entityManager.persist(table);
                entityManager.flush();
                return "Вы успешно заняли место✔";
            } else {
                Tables tables1 = user.getTables();
                tables1.setIs_taken(false);
                tables1.setUsers(null);
                entityManager.persist(tables1);
                user.setTables(table);
                table.setIs_taken(true);
                entityManager.persist(user);
                entityManager.persist(table);
                entityManager.flush();
                return "Вы успешло поменяли место сотрудника✔";
            }
        }
    }
    // осталось сделать хотя бы ещё функцию очистки стола и занятия стола для администратора - takeTable(String number, String login), clearTable(String number, String login)
    @Transactional
    public String clearTable(Long chatId) {
        Users user = findUserByChatId(chatId);
        Tables table = user.getTables();
        table.setUsers(null);
        table.setIs_taken(false);
        entityManager.persist(table);
        user.setTables(null);
        entityManager.persist(user);
        entityManager.flush();
        return "Вы успешно освободили место✔";
    }
    @Transactional
    public String clearTable(String login) {
        TypedQuery<Users> query = entityManager.createQuery("SELECT u from User u WHERE u.login = :login", Users.class).setParameter("login", login);
        List<Users> users = query.getResultList();
        Users user = users.getFirst();
        Tables table = user.getTables();
        if (user.getTables() == null) {
            return "У пользователя и так ничего не занято";
        }
        table.setUsers(null);
        table.setIs_taken(false);
        entityManager.persist(table);
        user.setTables(null);
        entityManager.persist(user);
        entityManager.flush();
        return "Вы успешно освободили место✔";
    }
    @Transactional
    public Map<String, ArrayList<String>> showAllUsers() {
        Map<String, ArrayList<String>> all = new HashMap<>();
        all.put("Адм", new ArrayList<>());
        all.put("Сотр", new ArrayList<>());
        TypedQuery<Users> query = entityManager.createQuery("SELECT u FROM User u", Users.class);
        List<Users> users = query.getResultList();
        for (Users u : users) {
            if (u.isIs_admin()) {
                all.get("Адм").add(u.getLogin() + " - " + u.getPassword());
            } else {
                all.get("Сотр").add(u.getLogin() + " - " + u.getPassword());
            }
        }
        return all;
    }
    @Transactional
    public void changeSession(Long chatId, String session) {
        Users user = findUserByChatId(chatId);
        TypedQuery<Session> query1 = entityManager.createQuery("SELECT s from UserSession s WHERE name = :name", Session.class).setParameter("name", session);
        Session session1 = query1.getSingleResult();
        user.setAction(session1);
        entityManager.persist(user);
        entityManager.flush();
    }
    @Transactional
    public void changeSession(String login, String session) {
        TypedQuery<Users> query = entityManager.createQuery("SELECT u from User u WHERE login = :login", Users.class).setParameter("login", login);
        List<Users> users = query.getResultList();
        Users user = users.getFirst();
        TypedQuery<Session> query1 = entityManager.createQuery("SELECT s from UserSession s WHERE name = :name", Session.class).setParameter("name", session);
        Session session1 = query1.getSingleResult();
        user.setAction(session1);
        entityManager.persist(user);
        entityManager.flush();
    }

    @Transactional
    public String getSession(Long chatId) {
        Users user = findUserByChatId(chatId);
        if (user == null) {
            return null;
        } else {
            return user.getAction().getName();
        }
    }
    @Transactional
    public boolean Is_registered(Long chatId) {
        Users user = findUserByChatId(chatId);
        if (user == null) {
            return false;
        }
        return user.isIs_registered();
    }
    @Transactional
    public void changeRegistered(Long chatId, boolean r) {
        Users user = findUserByChatId(chatId);
        user.setIs_registered(r);
        entityManager.persist(user);
        entityManager.flush();
    }
    @Transactional
    public boolean checkAdmin(Long chatId) {
        Users user = findUserByChatId(chatId);
        if (user == null) {
            return false;
        }
        return user.isIs_admin();
    }
    @Transactional
    public boolean checkTable(Long chatId) {
        Users user = findUserByChatId(chatId);
        if (user == null) {
            return false;
        }
        return user.getTables() != null;
    }
    @Transactional
    public void setChatId(Long chatId, String login) {
        TypedQuery<Users> query = entityManager.createQuery("SELECT u from User u WHERE login = :login", Users.class).setParameter("login", login);
        List<Users> users = query.getResultList();
        Users user = users.getFirst();
        user.setChatId(chatId);
        entityManager.persist(user);
        entityManager.flush();
    }
    @Transactional
    public void ensureUserExists(Long chatId) {
        Users existingUser = findUserByChatId(chatId);
        if (existingUser != null) {
            return;
        }

        // Создаем только если нет пользователя
        TypedQuery<Session> query = entityManager.createQuery(
                "SELECT s FROM UserSession s WHERE s.name = 'Ничего'", Session.class);
        Session session = query.getSingleResult();

        Users user = new Users();
        user.setChatId(chatId);
        user.setAction(session);
        user.setIs_registered(false);
        user.setIs_admin(false);
        user.setCreated(LocalDate.now());
        user.setLogin("temp_" + chatId);
        user.setPassword("temp");
        entityManager.persist(user);
        entityManager.flush();
    }
    @Transactional
    public Map<Integer, ArrayList<String>> getAllUntakenTables() {
        Map<Integer, ArrayList<String>> all = new LinkedHashMap<>();
        List<Tables> result = entityManager.createQuery("SELECT t FROM UserTables t WHERE t.is_taken = false", Tables.class).getResultList();
        ArrayList<Integer> floors = new ArrayList<>();
        for (Tables table : result) {
            floors.add(table.getFloor());
        }
        Collections.sort(floors);
        Set<Integer> uniqFloors = new LinkedHashSet<>(floors);
        for (Integer n : uniqFloors) {
            all.put(n, new ArrayList<>());
        }
        for (Tables table : result) {
            Integer floor = table.getFloor();
            all.get(floor).add(table.getNumber());
        }
        return all;
    }
    public void setAdminSession(Long chatId, boolean isAdmin) {

        adminSessions.put(chatId, isAdmin);
    }
    public void setUserSaves(Long chatId, String login) {
        loginSaves.put(chatId, login);
    }
    public String getUserSaves(Long chatId) {
        return loginSaves.getOrDefault(chatId, null);
    }
    public boolean getAdminSession(Long chatId) {
        return adminSessions.getOrDefault(chatId, false);
    }
    @Transactional
    public boolean findUserByLogin(String login) {
        try {
            TypedQuery<Users> query = entityManager.createQuery("SELECT u from User u WHERE login = :login", Users.class).setParameter("login", login);
            Users user = query.getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }
    Users findUserByChatId(Long chatId) {
        try {
            TypedQuery<Users> query = entityManager.createQuery("SELECT u from User u WHERE chatId = :chatId", Users.class).setParameter("chatId", chatId);
            Users user = query.getSingleResult();
            if (user.getAction() == null) {
                TypedQuery<Session> query1 = entityManager.createQuery("SELECT s FROM UserSession s WHERE s.name = 'Ничего'", Session.class);
                user.setAction(query1.getSingleResult());
            }

            return user;
        } catch (NoResultException e) {
            return null;
        }

    }

}
