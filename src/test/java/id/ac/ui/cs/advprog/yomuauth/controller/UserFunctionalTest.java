package id.ac.ui.cs.advprog.yomuauth.controller;

import io.github.bonigarcia.seljup.SeleniumJupiter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Route;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SeleniumJupiter.class)
public class UserFunctionalTest {

    private final String BASE_URL = "http://localhost:3000";

    @Test
    void testProfileRedirectsToLoginWhenUnauthenticated(ChromeDriver driver) {
        driver.get(BASE_URL + "/profile/someuser");
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/login"));
        
        assertTrue(driver.getCurrentUrl().contains("/login"), "Harus di-redirect ke login jika tidak ada token");
    }

    @Test
    @Disabled("Aktifkan jika environment e2e (Backend + DB) sudah siap")
    void testProfileFormsAreRenderedForOwner(ChromeDriver driver) {
        driver.get(BASE_URL);
        String mockUserJson = "{\"id\":\"123\", \"username\":\"testuser\", \"fullName\":\"Test User\", \"role\":\"LEARNER\"}";
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.setItem('token', 'dummy-token');");
        js.executeScript("window.localStorage.setItem('user', '" + mockUserJson + "');");

        // Mock fetch API menggunakan JavaScript agar terhindar dari isu Selenium CDP (Chrome 148 vs Selenium 4.14)
        String mockFetchScript = "const originalFetch = window.fetch; " +
            "window.fetch = async function() { " +
            "  if (arguments[0] && arguments[0].includes('/api/user/username/testuser')) { " +
            "    return new Response(JSON.stringify(" + mockUserJson + "), {status: 200, headers: {'Content-Type': 'application/json'}}); " +
            "  } " +
            "  return originalFetch.apply(this, arguments); " +
            "};";
        js.executeScript(mockFetchScript);

        // Kunjungi halaman menggunakan client-side navigation (click link) agar fetch mock tidak ter-reset
        js.executeScript("const a = document.createElement('a'); a.href = '/profile/testuser'; document.body.appendChild(a); a.click();");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Memastikan form "Data Diri" (Update Profile) muncul
        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//label[contains(text(), 'Username')]/following-sibling::input")));
        WebElement fullNameInput = driver.findElement(By.xpath("//label[contains(text(), 'Nama Lengkap')]/following-sibling::input"));
        WebElement saveProfileBtn = driver.findElement(By.xpath("//button[contains(text(), 'Simpan Perubahan')]"));

        assertNotNull(usernameInput);
        assertNotNull(fullNameInput);
        assertNotNull(saveProfileBtn);

        // Memastikan form "Ganti Password" muncul
        WebElement oldPwdInput = driver.findElement(By.xpath("//label[contains(text(), 'Password Lama')]/following-sibling::input"));
        WebElement newPwdInput = driver.findElement(By.xpath("//label[contains(text(), 'Password Baru')]/following-sibling::input"));
        WebElement savePwdBtn = driver.findElement(By.xpath("//button[contains(text(), 'Simpan Password')]"));

        assertNotNull(oldPwdInput);
        assertNotNull(newPwdInput);
        assertNotNull(savePwdBtn);

        // Memastikan form/tombol "Hapus Akun" muncul
        WebElement deleteBtn = driver.findElement(By.xpath("//button[contains(text(), 'Hapus Akun Permanen')]"));
        assertNotNull(deleteBtn);
    }

    @Test
    @Disabled("Aktifkan jika environment e2e (Backend + DB) sudah siap")
    void testChangePasswordValidation(ChromeDriver driver) {
        driver.get(BASE_URL);
        String mockUserJson = "{\"id\":\"123\", \"username\":\"testuser\", \"fullName\":\"Test User\", \"role\":\"LEARNER\"}";
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.setItem('token', 'dummy-token');");
        js.executeScript("window.localStorage.setItem('user', '" + mockUserJson + "');");

        String mockFetchScript = "const originalFetch = window.fetch; " +
            "window.fetch = async function() { " +
            "  if (arguments[0] && arguments[0].includes('/api/user/username/testuser')) { " +
            "    return new Response(JSON.stringify(" + mockUserJson + "), {status: 200, headers: {'Content-Type': 'application/json'}}); " +
            "  } " +
            "  return originalFetch.apply(this, arguments); " +
            "};";
        js.executeScript(mockFetchScript);

        js.executeScript("const a = document.createElement('a'); a.href = '/profile/testuser'; document.body.appendChild(a); a.click();");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        WebElement oldPwdInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//label[contains(text(), 'Password Lama')]/following-sibling::input")));
        WebElement newPwdInput = driver.findElement(By.xpath("//label[contains(text(), 'Password Baru')]/following-sibling::input"));
        WebElement savePwdBtn = driver.findElement(By.xpath("//button[contains(text(), 'Simpan Password')]"));

        oldPwdInput.sendKeys("password123");
        newPwdInput.sendKeys("password123");
        savePwdBtn.click();

        WebElement errorMsg = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(text(), 'tidak boleh sama dengan')]")));
        assertNotNull(errorMsg, "Pesan error validasi password harusnya muncul");
    }

    @Test
    @Disabled("Aktifkan jika environment e2e (Backend + DB) sudah siap")
    void testProfileShowsErrorWhenApiFails(ChromeDriver driver) {
        driver.get(BASE_URL);
        String mockUserJson = "{\"id\":\"123\", \"username\":\"testuser\", \"fullName\":\"Test User\", \"role\":\"LEARNER\"}";
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.setItem('token', 'dummy-token');");
        js.executeScript("window.localStorage.setItem('user', '" + mockUserJson + "');");

        String mockFetchScript = "const originalFetch = window.fetch; " +
            "window.fetch = async function() { " +
            "  if (arguments[0] && arguments[0].includes('/api/user/username/testuser')) { " +
            "    return new Response(JSON.stringify({message: 'User not found'}), {status: 404, headers: {'Content-Type': 'application/json'}}); " +
            "  } " +
            "  return originalFetch.apply(this, arguments); " +
            "};";
        js.executeScript(mockFetchScript);

        js.executeScript("const a = document.createElement('a'); a.href = '/profile/testuser'; document.body.appendChild(a); a.click();");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        WebElement errorTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//h2[contains(text(), 'Oops!')]")));
        assertNotNull(errorTitle);
    }
}
