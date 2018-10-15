package ntut.edu.guide.crawljax.plugins;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vodalok on 2017/1/10.
 */
public class SupportInputsManager {
    private List<String> supportsInputTypes = Arrays.asList("text", "password", "email", "search", "url", "tel",
            "number", "checkbox", "file");
    //Please DO NOT add <input> into this list
    private List<String> supportsTag = Arrays.asList("textarea", "select");

    public SupportInputsManager(){

    }

    public SupportInputsManager(List<String> supportsInputTypes, List<String> supportsTag){
        this.supportsInputTypes = supportsInputTypes;
        this.supportsTag = supportsTag;
    }

    public final List<String> getSupportsInputTypes(){
        return this.supportsInputTypes;
    }

    public final List<String> getSupportsTag(){
        return this.supportsTag;
    }

    public void setSupportsInputTypes(List<String> newSupportInputType){
        this.supportsInputTypes = newSupportInputType;
    }

    public void setSupportsTag(List<String> newSupportsTag){
        this.supportsTag = newSupportsTag;
    }

    /***
     * Use default support list to build XPath.
     * @return xpath with support inputs and tags.
     */
    public String buildXPath(){
        return this.buildXPath(supportsInputTypes, supportsTag);
    }

    /***
     * Build a xpath that can find out all the given input type or tag.
     * @param supportsInputsList the supported input type.
     * @param supportsTag support tag except input
     * @return xpath that can find out all given input type or tag.
     */
    private String buildXPath(List<String> supportsInputsList, List<String> supportsTag){
        String xPathBase = "//*/";
        StringBuilder xPathBuilder = new StringBuilder(xPathBase);

        xPathBuilder.append("input");
        xPathBuilder.append("[]");
        StringBuilder anSupportedInputBuilder = new StringBuilder();
        for(int i = 0; i < supportsInputsList.size(); i++){
            anSupportedInputBuilder.append("@type=");
            anSupportedInputBuilder.append("'");
            anSupportedInputBuilder.append(supportsInputsList.get(i));
            anSupportedInputBuilder.append("'");
            if(i != supportsInputsList.size() - 1){
                anSupportedInputBuilder.append(" or ");
            }
        }
        anSupportedInputBuilder.append(" or not(@type)");

        xPathBuilder.insert(xPathBuilder.length() - 1, anSupportedInputBuilder);

        if(!supportsTag.isEmpty()){
            for(int i = 0; i < supportsTag.size(); i++){
                xPathBuilder.append("|");
                xPathBuilder.append(xPathBase);
                xPathBuilder.append(supportsTag.get(i));
            }
        }

        return xPathBuilder.toString();
    }
}
