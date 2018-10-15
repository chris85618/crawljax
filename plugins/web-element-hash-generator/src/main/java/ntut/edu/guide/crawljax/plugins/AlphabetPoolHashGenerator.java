package ntut.edu.guide.crawljax.plugins;

import java.util.Random;

/**
 * Created by vodalok on 2017/4/26.
 */
public class AlphabetPoolHashGenerator implements HashGenerator {
    private final char[] ALPHABET_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private int digit;

    public AlphabetPoolHashGenerator(int digit){
        this.digit = digit;
    }

    @Override
    public String generateHash() {
        StringBuilder hashCodeBuilder = new StringBuilder();
        Random random = new Random();

        for(int i = 0; i < digit; i++){
            int pos = random.nextInt(this.ALPHABET_POOL.length);
            char aCode = ALPHABET_POOL[pos];
            hashCodeBuilder.append(aCode);
        }

        return hashCodeBuilder.toString();
    }
}
