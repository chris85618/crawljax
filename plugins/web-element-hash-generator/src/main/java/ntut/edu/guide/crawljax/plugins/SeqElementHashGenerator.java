package ntut.edu.guide.crawljax.plugins;

import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by vodalok on 2017/4/27.
 */
public class SeqElementHashGenerator implements HashGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeqElementHashGenerator.class);

    private enum SUPPORT_TAGS {
        INPUT ("input"),
        TEXTAREA ("textarea"),
        SELECT("select");

        private final String tagName;

        SUPPORT_TAGS(String tagName){
            this.tagName = tagName;
        }


    }

    private WebElement element;
    private int elementSeq;

    public SeqElementHashGenerator(WebElement element, int elementSeq) {
        this.element = element;
        this.elementSeq = elementSeq;
    }

    @Override
    public String generateHash() {
        StringBuilder hashBuilder = new StringBuilder();

        String tagName = this.element.getTagName();
        String attributeString = this.getAttributeString();

        if(attributeString == null){
            LOGGER.warn("The element is not supported so cannot generate a hash string for it.");
            return null;
        }

        String synthesis = tagName + attributeString + this.elementSeq;
        LOGGER.info("Sum String: {}", synthesis);
        String digest = "";
        try{
            MessageDigest webElementDigest = MessageDigest.getInstance("MD5");
            webElementDigest.update(synthesis.getBytes());
            byte[] md5 = webElementDigest.digest();
            for(byte b:md5){
                digest += String.format("%02x", b & 0xff);
            }
        }catch (NoSuchAlgorithmException exception){
            LOGGER.error(exception.getMessage());
        }

        return digest;
    }

    private String getAttributeString(){
        String tagName = this.element.getTagName();
        try{
            SUPPORT_TAGS inputCase = SUPPORT_TAGS.valueOf(tagName.toUpperCase());
            switch (inputCase){
                case INPUT:
                    return getInputAttributeString();
                case TEXTAREA:
                    return  getTextareaAttributeString();
                case SELECT:
                    return getSelectAttributeString();
                default:
                    LOGGER.warn("The element is not supported so cannot generate an attribute string for it");
            }
        }catch (IllegalArgumentException exception){
            LOGGER.info("The type does not supported");
        }

        return null;
    }

    private String getInputAttributeString(){
        StringBuilder attributeStringBuilder = new StringBuilder();

        attributeStringBuilder
                .append(this.element.getAttribute("accept"))
                .append(this.element.getAttribute("align"))
                .append(this.element.getAttribute("autocomplete"))
                .append(this.element.getAttribute("autofocus"))
                .append(this.element.getAttribute("disabled"))
                .append(this.element.getAttribute("max"))
                .append(this.element.getAttribute("min"))
                .append(this.element.getAttribute("pattern"))
                .append(this.element.getAttribute("placeholder"))
                .append(this.element.getAttribute("type"))
                .append(this.element.getAttribute("size"))
                .append(this.element.getAttribute("title"));

        return attributeStringBuilder.toString();
    }

    private String getTextareaAttributeString(){
        StringBuilder attributeStringBuilder = new StringBuilder();

        attributeStringBuilder
                .append(this.element.getAttribute("rows"))
                .append(this.element.getAttribute("cols"))
                .append(this.element.getAttribute("autofocus"))
                .append(this.element.getAttribute("disabled"))
                .append(this.element.getAttribute("form"))
                .append(this.element.getAttribute("maxlength"))
                .append(this.element.getAttribute("wrap"))
                .append(this.element.getAttribute("placeholder"))
                .append(this.element.getAttribute("type"))
                .append(this.element.getAttribute("title"));

        return attributeStringBuilder.toString();
    }

    private String getSelectAttributeString(){
        StringBuilder attributeStringBuilder = new StringBuilder();
        attributeStringBuilder
                .append(this.element.getAttribute("multiple"))
                .append(this.element.getAttribute("size"))
                .append(this.element.getAttribute("autofocus"))
                .append(this.element.getAttribute("disabled"))
                .append(this.element.getAttribute("form"))
                .append(this.element.getAttribute("title"));

        return attributeStringBuilder.toString();
    }
}
