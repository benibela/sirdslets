
import java.util.logging.Level;
import java.util.logging.Logger;


public class AccurateTiming {
	//Java timers are strange, their accurancy fluctuates between 1ns - 55ms, and they can run backward.
	//This timer class will never run backward, and has an accurancy between 0.5ms - 27ms.
	private long timerMillisStart, timerNanoStart;
	private long timerMillisFail, timerNanoFail;

	private long timerMillisLast, timerNanoLast, timerTotalLast;

	public AccurateTiming() {
		timerMillisStart = System.currentTimeMillis();
		timerNanoStart = System.nanoTime();
		timerMillisLast = timerMillisStart;
		timerNanoLast = timerNanoStart;
		timerMillisFail = 0;
		timerNanoFail = 0;
	}


	public long getTiming(){
		long millisNew = System.currentTimeMillis() - timerMillisStart;
		long nanoNew = System.nanoTime() - timerNanoStart;
		if (millisNew < timerMillisLast) timerMillisFail += (timerMillisLast - millisNew);
		if (nanoNew < timerNanoLast) timerNanoFail += timerNanoLast - nanoNew;

		timerMillisLast = millisNew;
		timerNanoLast = nanoNew;

		boolean millisFailed = timerMillisFail > 1000*2; //allow 5*1s backward for millis
		boolean nanosFailed = timerNanoFail > 100*1000000*5; //allow 5*.1s backward for nanos

		long newTime = timerTotalLast;
		if (millisFailed == nanosFailed) newTime = (millisNew + nanoNew / 1000000) / 2;
		else if (nanosFailed) newTime = millisNew;
		else newTime = nanoNew / 1000000;

		if (timerTotalLast > newTime) newTime = timerTotalLast;
		timerTotalLast = newTime;

		return timerTotalLast;
	}


	int threadSleepFail = 0;
	int threadSleep2Fail = 0;
	int threadSleep3Fail = 0;
	public void threadSleep(long ms){
		if (ms<=1) return;
		long startTime = getTiming();
		if (threadSleepFail < 20*20) {
			try {
				Thread.sleep(ms - 1);
			} catch (InterruptedException ex) {
				threadSleepFail += 5;
			}
			if (Math.abs(getTiming() - startTime - ms) > 5) threadSleepFail +=10;
			else threadSleepFail--;
		} else if (threadSleep2Fail < 20*20) {
			while (getTiming() - startTime < ms)
				try {
					Thread.sleep(1);
				} catch (InterruptedException ex) {
				}
			if (Math.abs(getTiming() - startTime - ms) > 5) threadSleep2Fail +=10;
			else threadSleep2Fail--;
		} else if (threadSleep3Fail < 20*20) {
			while (getTiming() - startTime < ms) Thread.yield();
			if (Math.abs(getTiming() - startTime - ms) > 5) threadSleep3Fail +=10;
			else threadSleep3Fail--;
		} else while (getTiming() - startTime < ms);
	}
}
