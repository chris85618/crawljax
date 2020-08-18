package ntut.edu.aiguide.crawljax.plugins.mockObject;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.state.Element;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.forms.FormInput;
import com.crawljax.util.DomUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.WebElement;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class MockBrowser implements EmbeddedBrowser {
    @Override
    public boolean isInteractive(String identification) {
        return true;
    }

    @Override
    public WebElement getWebElement(Identification identification) {
        return null;
    }

    @Override
    public void goToUrl(URI url) {

    }

    @Override
    public boolean fireEventAndWait(Eventable event) throws ElementNotVisibleException, InterruptedException {
        return false;
    }

    @Override
    public String getStrippedDom() {
        return null;
    }

    @Override
    public String getUnStrippedDom() {
        return null;
    }

    @Override
    public String getDom() {
        return null;
    }

    @Override
    public String getStrippedDomWithoutIframeContent() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void closeOtherWindows() {

    }

    @Override
    public void goBack() {

    }

    @Override
    public boolean input(Identification identification, String text) throws CrawljaxException {
        return false;
    }

    @Override
    public Object executeJavaScript(String script) throws CrawljaxException {
        return null;
    }

    @Override
    public boolean isVisible(Identification identification) {
        return false;
    }

    @Override
    public String getCurrentUrl() {
        return null;
    }

    @Override
    public FormInput getInputWithRandomValue(FormInput inputForm) {
        return null;
    }

    @Override
    public String getFrameDom(String iframeIdentification) {
        return null;
    }

    @Override
    public boolean elementExists(Identification identification) {
        return false;
    }

    @Override
    public void saveScreenShot(File file) throws CrawljaxException {

    }

    @Override
    public byte[] getScreenShot() throws CrawljaxException {
        return new byte[0];
    }

    @Override
    public void deleteAllCookies() {

    }
}
