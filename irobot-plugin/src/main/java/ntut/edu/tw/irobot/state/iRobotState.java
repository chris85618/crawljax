package ntut.edu.tw.irobot.state;


import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;

import java.util.Objects;


public class iRobotState implements State {
    private final StateVertex source;
    private ImmutableList<String> coverageVector;

    public iRobotState(StateVertex state) {
        this.coverageVector = ImmutableList.of();
        this.source = state;
    }

    public StateVertex getSource() {
        return source;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public String getUrl() {
        return source.getUrl();
    }

    @Override
    public String getDom() {
        return source.getStrippedDom();
    }

    @Override
    public int getId() {
        return source.getId();
    }

    @Override
    public void setCoverageVector(ImmutableList<String> coverageVector) {
        this.coverageVector = coverageVector;
    }

    @Override
    public ImmutableList<String> getCoverageVector() {
        return coverageVector;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof iRobotState) {
            iRobotState that = (iRobotState) object;
            return Objects.equals(source.getStrippedDom(), that.getDom());
        }
        return false;
    }

    @Override
    public String toString() {
        return source.toString();
    }
}
