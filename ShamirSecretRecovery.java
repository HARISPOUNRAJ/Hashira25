import java.io.*;
import java.math.BigInteger;
import java.util.*;
import org.json.*;

public class ShamirSecretRecovery {

    static class Share {
        int index;
        BigInteger value;
        Share(int index, BigInteger value) {
            this.index = index;
            this.value = value;
        }
    }

    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONObject json = new JSONObject(sb.toString());

            int n = json.getJSONObject("keys").getInt("n");
            int k = json.getJSONObject("keys").getInt("k");

            List<Share> shares = new ArrayList<>();
            for (String key : json.keySet()) {
                if (key.equals("keys")) continue;
                JSONObject obj = json.getJSONObject(key);
                int base = Integer.parseInt(obj.getString("base"));
                BigInteger value = new BigInteger(obj.getString("value"), base);
                shares.add(new Share(Integer.parseInt(key), value));
            }

            Share wrongShare = null;
            BigInteger secret = null;

            outer:
            for (int i = 0; i < shares.size(); i++) {
                for (int j = 0; j < shares.size(); j++) {
                    if (i == j) continue;

                    List<Share> candidateShares = new ArrayList<>();
                    for (int idx = 0; idx < shares.size(); idx++) {
                        if (idx != j) candidateShares.add(shares.get(idx));
                    }

                    List<Share> subset = candidateShares.subList(0, k);

                    try {
                        BigInteger computedSecret = lagrangeInterpolationAtZero(subset);
                        boolean consistent = true;
                        for (Share s : shares) {
                            if (!subset.contains(s)) {
                                BigInteger predicted = lagrangeInterpolationAtX(subset, s.index);
                                if (!predicted.equals(s.value)) {
                                    consistent = false;
                                    break;
                                }
                            }
                        }
                        if (consistent) {
                            secret = computedSecret;
                            wrongShare = shares.get(j);
                            break outer;
                        }
                    } catch (ArithmeticException e) {}
                }
            }

            if (secret != null && wrongShare != null) {
                System.out.println("Secret: " + secret);
                System.out.println("Wrong Share: " + wrongShare.index + " (" + wrongShare.value + ")");
            } else {
                System.out.println("Failed to recover secret or identify wrong share.");
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static BigInteger lagrangeInterpolationAtZero(List<Share> shares) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < shares.size(); i++) {
            BigInteger xi = BigInteger.valueOf(shares.get(i).index);
            BigInteger yi = shares.get(i).value;
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < shares.size(); j++) {
                if (i == j) continue;
                BigInteger xj = BigInteger.valueOf(shares.get(j).index);
                numerator = numerator.multiply(xj.negate());
                denominator = denominator.multiply(xi.subtract(xj));
            }

            result = result.add(yi.multiply(numerator).divide(denominator));
        }
        return result;
    }

    private static BigInteger lagrangeInterpolationAtX(List<Share> shares, int x) {
        BigInteger result = BigInteger.ZERO;
        BigInteger X = BigInteger.valueOf(x);

        for (int i = 0; i < shares.size(); i++) {
            BigInteger xi = BigInteger.valueOf(shares.get(i).index);
            BigInteger yi = shares.get(i).value;
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < shares.size(); j++) {
                if (i == j) continue;
                BigInteger xj = BigInteger.valueOf(shares.get(j).index);
                numerator = numerator.multiply(X.subtract(xj));
                denominator = denominator.multiply(xi.subtract(xj));
            }
            result = result.add(yi.multiply(numerator).divide(denominator));
        }
        return result;
    }
}
