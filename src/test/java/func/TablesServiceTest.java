package func;

import com.example.T2bleBot.T2bleBotApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = T2bleBotApplication.class)
@Transactional
public class TablesServiceTest {

    @Autowired
    private TablesService tablesService;

    @Test
    public void quickTest() {
        // Генерируем уникальный логин
        String testLogin = "test_" + System.currentTimeMillis();

        // Вызываем вашу функцию
        String result = tablesService.createAccount(false, testLogin, "mypassword");

        // Выводим результат
        System.out.println("=========================================");
        System.out.println("Результат: " + result);
        System.out.println("=========================================");

        if (result.equals("Пользователь добавлен✔")) {
            System.out.println("✅ Функция работает корректно!");
        } else {
            System.out.println("❌ Функция вернула: " + result);
        }
    }
}