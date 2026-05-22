package id.ac.ui.cs.advprog.yomuauth.controller;

import io.github.bonigarcia.seljup.SeleniumJupiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SeleniumJupiter.class)
public class AuthFunctionalTest {

    private final String BASE_URL = "http://localhost:3000";

    /**
     * Test ini hanya dijalankan secara LOKAL melalui perintah:
     * ./gradlew functionalTest
     * 
     * Syarat: Aplikasi Frontend harus sudah menyala di localhost:3000
     */
    @Test
    void testFrontendIsAccessible(ChromeDriver driver) {
        driver.get(BASE_URL);
        String pageSource = driver.getPageSource();
        assertNotNull(pageSource);
        assertTrue(pageSource.contains("<html"));
        System.out.println("Selenium berhasil membuka halaman frontend lokal!");
    }

    @Test
    void testLoginPageElementsAndLayout(ChromeDriver driver) {
        driver.get(BASE_URL + "/login");
        
        WebElement identifierInput = driver.findElement(By.name("identifier"));
        WebElement passwordInput = driver.findElement(By.name("password"));
        WebElement submitBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        
        assertNotNull(identifierInput);
        assertNotNull(passwordInput);
        assertNotNull(submitBtn);
        assertTrue(submitBtn.getText().contains("Masuk"));
        
        // Cek link ke halaman register
        WebElement registerLink = driver.findElement(By.xpath("//a[@href='/register']"));
        assertNotNull(registerLink);
    }

    @Test
    void testRegisterPageElementsAndLayout(ChromeDriver driver) {
        driver.get(BASE_URL + "/register");
        
        WebElement fullNameInput = driver.findElement(By.name("fullName"));
        WebElement usernameInput = driver.findElement(By.name("username"));
        WebElement emailInput = driver.findElement(By.name("email"));
        WebElement passwordInput = driver.findElement(By.name("password"));
        WebElement submitBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        
        assertNotNull(fullNameInput);
        assertNotNull(usernameInput);
        assertNotNull(emailInput);
        assertNotNull(passwordInput);
        assertNotNull(submitBtn);
        assertTrue(submitBtn.getText().contains("Daftar Sekarang"));
        
        // Cek link ke halaman login
        WebElement loginLink = driver.findElement(By.xpath("//a[@href='/login']"));
        assertNotNull(loginLink);
    }

    @Test
    void testNavigationBetweenLoginAndRegister(ChromeDriver driver) throws InterruptedException {
        driver.get(BASE_URL + "/login");
        
        WebElement registerLink = driver.findElement(By.xpath("//a[@href='/register']"));
        registerLink.click();
        
        // Tunggu sebentar agar page update
        Thread.sleep(1000);
        assertTrue(driver.getCurrentUrl().contains("/register"));
        
        WebElement loginLink = driver.findElement(By.xpath("//a[@href='/login']"));
        loginLink.click();
        
        Thread.sleep(1000);
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void testLoginWithEmptyFields(ChromeDriver driver) {
        driver.get(BASE_URL + "/login");
        
        WebElement submitBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        submitBtn.click();
        
        // Karena ada atribut `required` di input form, browser akan menahan submit
        // Verifikasi bahwa URL masih tetap di /login
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void testRegisterWithEmptyFields(ChromeDriver driver) {
        driver.get(BASE_URL + "/register");
        
        WebElement submitBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        submitBtn.click();
        
        // Karena ada atribut `required` di input form, browser akan menahan submit
        // Verifikasi bahwa URL masih tetap di /register
        assertTrue(driver.getCurrentUrl().contains("/register"));
    }
}
