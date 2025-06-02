package ntut.edu.crawlguider.crawljax.plugins.domain;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class EditDistanceComparator {

    private double threshold = 1;

    public EditDistanceComparator(double threshold) {
        this.threshold = threshold;
    }

    public boolean isEquivalent(String originalDom, String newDom) {
        double thresholdCoefficient = getThreshold(originalDom, newDom, threshold);
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance((int) thresholdCoefficient);
        int dist = levenshteinDistance.apply(originalDom, newDom);
        return dist != -1 && dist <= thresholdCoefficient;
    }

    double getThreshold(String x, String y, double p) {
        return 2 * Math.max(x.length(), y.length()) * (1 - p);
    }
}
