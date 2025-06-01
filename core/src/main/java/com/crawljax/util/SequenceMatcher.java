package com.crawljax.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Comparator;

// TODO: review this code
public class SequenceMatcher<T> {
    private List<T> a;
    private List<T> b;
    private Map<T, List<Integer>> b2j;
    private Set<T> bjunk;
    private List<Match> matchingBlocks = null;
    private List<Opcode> opcodes = null;
    private JunkPredicate<T> isJunk;

    public SequenceMatcher() {
        this(null, new ArrayList<T>(), new ArrayList<T>());
    }

    public SequenceMatcher(JunkPredicate<T> isJunk, List<T> a, List<T> b) {
        this.isJunk = isJunk;
        this.a = a;
        this.b = b;
        this.bjunk = new HashSet<>();
        this.b2j = new HashMap<>();

        for (int i = 0; i < b.size(); i++) {
            T element = b.get(i);
            if (isJunk != null && isJunk.isJunk(element)) {
                bjunk.add(element);
                continue;
            }
            b2j.computeIfAbsent(element, k -> new ArrayList<>()).add(i);
        }
    }

    public void setSeqs(List<T> a, List<T> b) {
        setSeq1(a);
        setSeq2(b);
    }

    public void setSeq1(List<T> a) {
        if (this.a == a) return;
        this.a = a;
        this.matchingBlocks = null;
        this.opcodes = null;
    }

    public void setSeq2(List<T> b) {
        if (this.b == b) return;
        this.b = b;
        this.matchingBlocks = null;
        this.opcodes = null;
        chainB();
    }

    private void chainB() {
        b2j = new HashMap<>();
        for (int i = 0; i < b.size(); i++) {
            T elt = b.get(i);
            b2j.computeIfAbsent(elt, k -> new ArrayList<>()).add(i);
        }
    
        // 移除 junk 元素
        bjunk = new HashSet<>();
        if (isJunk != null) {
            for (T elt : new HashSet<>(b2j.keySet())) {
                if (isJunk.isJunk(elt)) {
                    bjunk.add(elt);
                    b2j.remove(elt);
                }
            }
        }
    
        // 可選功能：自動忽略太常見的元素（對應 autojunk）
        int n = b.size();
        boolean autojunk = true; // 若你需要控制它，也可改為類別變數
        if (autojunk && n >= 200) {
            int ntest = n / 100 + 1;
            Set<T> popular = new HashSet<>();
            for (Map.Entry<T, List<Integer>> entry : b2j.entrySet()) {
                if (entry.getValue().size() > ntest) {
                    popular.add(entry.getKey());
                }
            }
            for (T elt : popular) {
                b2j.remove(elt);
            }
        }
    }

    public Match findLongestMatch(int alo, int ahi, int blo, int bhi) {
        int besti = alo, bestj = blo, bestsize = 0;
        Map<Integer, Integer> j2len = new HashMap<>();

        for (int i = alo; i < ahi; i++) {
            T aElem = a.get(i);
            List<Integer> indexes = b2j.getOrDefault(aElem, Collections.emptyList());
            Map<Integer, Integer> newj2len = new HashMap<>();

            for (int j : indexes) {
                if (j < blo || j >= bhi) continue;
                int k = j2len.getOrDefault(j - 1, 0) + 1;
                newj2len.put(j, k);
                if (k > bestsize) {
                    besti = i - k + 1;
                    bestj = j - k + 1;
                    bestsize = k;
                }
            }
            j2len = newj2len;
        }

        // Extend backward
        while (besti > alo && bestj > blo &&
               !bjunk.contains(b.get(bestj - 1)) &&
               a.get(besti - 1).equals(b.get(bestj - 1))) {
            besti--;
            bestj--;
            bestsize++;
        }

        // Extend forward
        while (besti + bestsize < ahi && bestj + bestsize < bhi &&
               !bjunk.contains(b.get(bestj + bestsize)) &&
               a.get(besti + bestsize).equals(b.get(bestj + bestsize))) {
            bestsize++;
        }

        // Extend matching junk
        while (besti > alo && bestj > blo &&
               bjunk.contains(b.get(bestj - 1)) &&
               a.get(besti - 1).equals(b.get(bestj - 1))) {
            besti--;
            bestj--;
            bestsize++;
        }

        while (besti + bestsize < ahi && bestj + bestsize < bhi &&
               bjunk.contains(b.get(bestj + bestsize)) &&
               a.get(besti + bestsize).equals(b.get(bestj + bestsize))) {
            bestsize++;
        }

        return new Match(besti, bestj, bestsize);
    }

    public List<Match> getMatchingBlocks() {
        if (matchingBlocks != null) return matchingBlocks;

        List<Match> matches = new ArrayList<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.push(new int[]{0, a.size(), 0, b.size()});

        while (!queue.isEmpty()) {
            int[] bounds = queue.pop();
            int alo = bounds[0], ahi = bounds[1], blo = bounds[2], bhi = bounds[3];
            Match m = findLongestMatch(alo, ahi, blo, bhi);
            if (m.size > 0) {
                matches.add(m);
                queue.push(new int[]{alo, m.a, blo, m.b});
                queue.push(new int[]{m.a + m.size, ahi, m.b + m.size, bhi});
            }
        }

        matches.sort(Comparator.comparingInt((Match m) -> m.a)
                               .thenComparingInt(m -> m.b));

        // Merge adjacent
        List<Match> nonAdjacent = new ArrayList<>();
        int i1 = 0, j1 = 0, k1 = 0;
        for (Match m : matches) {
            if (i1 + k1 == m.a && j1 + k1 == m.b) {
                k1 += m.size;
            } else {
                if (k1 != 0) nonAdjacent.add(new Match(i1, j1, k1));
                i1 = m.a;
                j1 = m.b;
                k1 = m.size;
            }
        }
        if (k1 != 0) nonAdjacent.add(new Match(i1, j1, k1));
        nonAdjacent.add(new Match(a.size(), b.size(), 0));

        this.matchingBlocks = nonAdjacent;
        return matchingBlocks;
    }

    public int getTotalMatchSize() {
        int total = 0;
        for (Match m : getMatchingBlocks()) {
            total += m.size;
        }
        return total;
    }

    public double ratio() {
        final int length = a.size() + b.size();
        if (length == 0) {
            return 1.0;
        }
        final int matches = this.getTotalMatchSize();
        return 2.0 * matches / (double) length;
    }

    public List<Opcode> getOpcodes() {
        if (opcodes != null) return opcodes;

        List<Opcode> result = new ArrayList<>();
        int i = 0, j = 0;

        for (Match m : getMatchingBlocks()) {
            if (i < m.a && j < m.b) {
                result.add(new Opcode(Opcode.Tag.REPLACE, i, m.a, j, m.b));
            } else if (i < m.a) {
                result.add(new Opcode(Opcode.Tag.DELETE, i, m.a, j, j));
            } else if (j < m.b) {
                result.add(new Opcode(Opcode.Tag.INSERT, i, i, j, m.b));
            }

            if (m.size > 0) {
                result.add(new Opcode(Opcode.Tag.EQUAL, m.a, m.a + m.size, m.b, m.b + m.size));
            }

            i = m.a + m.size;
            j = m.b + m.size;
        }

        this.opcodes = result;
        return result;
    }

    public interface JunkPredicate<T> {
        boolean isJunk(T value);
    }

    public static class Match {
        public final int a, b, size;
        public Match(int a, int b, int size) {
            this.a = a;
            this.b = b;
            this.size = size;
        }
    }

    public static class Opcode {
        public final Tag tag;
        public final int i1, i2, j1, j2;

        public enum Tag {
            EQUAL, INSERT, DELETE, REPLACE;

            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }

        public Opcode(Tag tag, int i1, int i2, int j1, int j2) {
            this.tag = tag;
            this.i1 = i1;
            this.i2 = i2;
            this.j1 = j1;
            this.j2 = j2;
        }

        @Override
        public String toString() {
            return String.format("%7s a[%d:%d] b[%d:%d]", tag.toString(), i1, i2, j1, j2);
        }
    }
}
