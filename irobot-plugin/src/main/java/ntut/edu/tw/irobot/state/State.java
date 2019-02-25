package ntut.edu.tw.irobot.state;


import com.google.common.collect.ImmutableList;

/**
 * This class is an abstract State, and will be used by learning.
 */
public interface State {

    /**
     * Retrieve the name of the State
     *
     * @return the name of the State
     */
    String getName();

    /**
     * @return the url
     */
    String getUrl();

    /**
     * @return the dom.
     */
    String getDom();

    /**
     * @return the id.
     */
    int getId();

    /**
     * @param coverageVector
     *                  ImmutableList that the current state coverage
     */
    void setCoverageVector(ImmutableList<String> coverageVector);

    /**
     * @return ImmutableList that the current state coverage
     */
    ImmutableList<String> getCoverageVector();
}
