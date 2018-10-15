package ntut.edu.guide.crawljax.plugins;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vodalok on 2017/4/27.
 */
public class SeqElementHashGeneratorTest {
    private WebDriver driver;
    private Path path;

    @Before
    public void setUp() throws URISyntaxException{
        URL testHTMLUrl = Thread.currentThread().getContextClassLoader().getResource("testHash.html");
        path = Paths.get(testHTMLUrl.toURI());
    }

    @Test
    public void testChromeDriverInputHashSame() throws InterruptedException{
        this.driver = new ChromeDriver();
        driver.get("file:///" + path.toAbsolutePath());

        //First time visit

        List<WebElement> inputElements = driver.findElements(By.tagName("input"));
        List<String> hashes = new ArrayList<>();

        for(WebElement anElement: inputElements){
            if(anElement.getAttribute("id").isEmpty() && anElement.getAttribute("name").isEmpty()){
                HashGenerator generator = new SeqElementHashGenerator(anElement, inputElements.indexOf(anElement));
                String generatedHash = generator.generateHash();
                System.out.print("Generated hash: ");
                System.out.println(generatedHash);
                hashes.add(generatedHash);
            }
        }

        //driver.close();
        Thread.sleep(2000);
        //Second time visit
        driver.get("file:///" + path.toAbsolutePath());
        inputElements = driver.findElements(By.tagName("input"));

        int hashSeq = 0;
        for(WebElement anElement: inputElements){
            if(anElement.getAttribute("id").isEmpty() && anElement.getAttribute("name").isEmpty()){
                int index = inputElements.indexOf(anElement);
                HashGenerator generator = new SeqElementHashGenerator(anElement, index);
                Assert.assertEquals("Hash not same!", hashes.get(hashSeq), generator.generateHash());
                hashSeq++;
            }
        }

    }

    @After
    public void tearDown(){
        if(this.driver != null){
            this.driver.quit();
        }
    }
}
