package ntut.edu.tw.irobot.action;


/**
 * This class is prepare for learning
 * maybe there will be another platform want to communicate with the learning machine
 */
public interface Action {
    /**
     * @return the name of the action
     */
    String getName();

    /**
     * @return the type of the action
     */
    String getType();

    /**
     * @return the source object
     */
    Object getSource();
}

