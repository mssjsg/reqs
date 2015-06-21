package reqs;

import org.junit.Assert;

/**
 * Created by maksing on 21/6/15.
 */
public class ReqsTester {
    private final int[] expectValues;
    private final int[] actualValues;

    private final int sleepInterval;
    private final int timeout;

    public ReqsTester(int[] expectValues) {
        this(expectValues, 20, 1000);
    }

    public ReqsTester(int[] expectValues, int sleepInterval, int timeout) {
        this.expectValues = new int[expectValues.length];
        this.actualValues = new int[expectValues.length];
        for (int i : expectValues) {
            this.expectValues[expectValues.length - 1] = i;
            this.actualValues[expectValues.length - 1] = -1;
        }
        this.sleepInterval = sleepInterval;
        this.timeout = timeout;
    }

    private boolean validate() {
        for (int i = 0; i < expectValues.length; i++) {
            if (expectValues[i] != getActualValue(i)) {
                return false;
            }
        }
        return true;
    }

    public synchronized int getActualValue(int pos) {
        return actualValues[pos];
    }

    public synchronized void setActualValue(int pos, int value) {
        actualValues[pos] = value;
    }

    public void check() {
        long startTime = System.currentTimeMillis();
        while (!validate()) {
            if (System.currentTimeMillis() - startTime > timeout) {
                String actual = "";
                String expect = "";
                for (int i = 0; i < expectValues.length; i++) {
                    actual = actual + this.expectValues[i] + ", ";
                    expect = expect + this.actualValues[i] + ", ";
                }

                Assert.fail("Time out!! actual:[" + actual + "], expect:[" + expect + "]");
                break;
            }
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
