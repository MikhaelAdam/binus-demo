package com.automate.binusdemo;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BinusTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        driver = new FirefoxDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        login(); // every test starts from a logged-in state
    }

    private void login() {
        String username = System.getenv("EMAIL");
        String password = System.getenv("PASS");
        assertNotNull(username, "BINUS_USER env var not set");
        assertNotNull(password, "BINUS_PASS env var not set");

        driver.get("https://binusmaya.binus.ac.id/login");

        WebElement emailField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='email']"))
        );
        emailField.sendKeys(username);
        driver.findElement(By.cssSelector("input[type='submit']")).click();

        WebElement passwordField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name='passwd']"))
        );
        passwordField.sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit']")).click();

        WebElement yesButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("input[type='submit'][value='Yes']"))
        );
        yesButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//p[contains(@class,'MuiTypography-root') and normalize-space(text())='My Dashboard']")
        ));
    }

    @Test
    public void dashboardLoadsAfterLogin() {
        WebElement header = driver.findElement(
                By.xpath("//p[contains(@class,'MuiTypography-root') and normalize-space(text())='My Dashboard']")
        );
        assertTrue(header.isDisplayed());
    }

    @Test
    public void openCurrentSessionPPT() throws InterruptedException {
        String originalWindow = driver.getWindowHandle();
        WebElement openButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'MuiButton-containedInfo') and contains(@class, 'css-mi8imr')]")
        ));
        openButton.click();
        wait.until(d -> d.getWindowHandles().size() > 1);

        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(originalWindow)) {
                driver.switchTo().window(handle);
                break;
            }
        }

        // Handle clicking the "Retry" button if exist here
        List<WebElement> allButtons = driver.findElements(By.tagName("button"));
        for (WebElement button : allButtons) {
            if (button.getText().trim().equalsIgnoreCase("retry")) {
                if (button.isDisplayed()) {
                    button.click();
                }
                break;
            }
        }

        List<WebElement> elementList = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("span.text-white"))
        );

        int targetOccurrence = 5;
        int targetIndex = targetOccurrence - 1;

        if (elementList.size() > targetIndex) {
            elementList.get(targetIndex).click();
        } else {
            fail("Target occurrence not found. Total found: " + elementList.size());
        }

        Thread.sleep(5000);

        List<WebElement> saveButtons = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("button.C--button.type--1.font-size-13px.-theme-primary")
                )
        );

        assertFalse(saveButtons.isEmpty(), "The button with the specified class does not exist on the page.");

        WebElement saveButton = saveButtons.get(1);
        String buttonText = saveButton.getText().trim().toLowerCase();

        assertEquals("save a copy", buttonText,
                "Expected button text 'save a copy' but found: '" + buttonText + "'");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) driver.quit();
    }
}