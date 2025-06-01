package ntut.edu.aiguide.crawljax.plugins.domain.SequenceMatcher;

import java.util.List;

public class ListSimilarity<T> {
    SequenceMatcher<T> matcher;

    public ListSimilarity() {
        this.matcher = new SequenceMatcher<T>();
    }

    public double get(List<T> a, List<T> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        } else if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        this.matcher.setSeq1(a);
        this.matcher.setSeq2(b);
        return this.matcher.ratio();
    }
}
