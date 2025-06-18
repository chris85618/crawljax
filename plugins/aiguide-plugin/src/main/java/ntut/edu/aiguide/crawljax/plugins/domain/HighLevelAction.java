package ntut.edu.aiguide.crawljax.plugins.domain;

import java.util.ArrayList;
import java.util.List;

public class HighLevelAction {
        List<Action> actionSequence;

        public HighLevelAction() {
            this.actionSequence = new ArrayList<>();
        }

        public HighLevelAction(final int size) {
            this.actionSequence = new ArrayList<>(size);
        }

        public HighLevelAction(final Action action) {
            this(1);
            this.actionSequence.add(action);
        }

        public HighLevelAction(final List<Action> actionSequence) {
            this(actionSequence.size());
            this.add(actionSequence);
        }

        public HighLevelAction(final HighLevelAction actionSequence) {
            this(actionSequence.size());
            this.add(actionSequence);
        }

        public void add(final Action action) {
            this.actionSequence.add(action);
        }

        public void add(final List<Action> actionSequence) {
            this.actionSequence.addAll(actionSequence);
        }

        public void add(final HighLevelAction actionSequence) {
            this.add(actionSequence.getActionSequence());
        }

        public List<Action> getActionSequence() {
            return this.actionSequence;
        }

        public int size() {
            return this.actionSequence.size();
        }    
}
