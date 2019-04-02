package com.mazhangjing.lab.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @implNote
 * 2019-04-02
 * WaveUtil 用来提供纯音，playForDuration 用来生成连续或者随机的指定声音频率数、声音时长、声音总时长、声音间隔的纯音
 */
public class WaveUtil {

    public static final int SAMPLE_RATE = 16 * 1024;

    private static byte[] createSinWaveBuffer(double freq, int ms) {
        int samples = (int)((ms * SAMPLE_RATE) / 1000);
        byte[] output = new byte[samples];
        double period = (double)SAMPLE_RATE / freq;
        for (int i = 0; i < output.length; i++) {
            double angle = 2.0 * Math.PI * i / period;
            output[i] = (byte)(Math.sin(angle) * 127f);  }
        return output;
    }

    public static void playForDuration(int soundFrequency, int soundDurationMs, int allDurationMS, int spaceMS)
            throws InterruptedException, LineUnavailableException {
        final AudioFormat af = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);
        SourceDataLine line = AudioSystem.getSourceDataLine(af);
        line.open(af, SAMPLE_RATE);
        line.start();
        long startTime = System.nanoTime();
        double currentTime = 0;
        while (currentTime < allDurationMS) {
            byte [] toneBuffer = createSinWaveBuffer(soundFrequency, soundDurationMs);
            line.write(toneBuffer, 0, toneBuffer.length);
            TimeUnit.MILLISECONDS.sleep(spaceMS);
            currentTime = (System.nanoTime() - startTime)/1000_000.0;
        }
        line.drain();
        line.close();
    }

    public static void playForDuration(int soundFrequency, int soundDurationMs,
                                         int allDurationMS, int randomSpaceFrom, int randomSpaceTo)
            throws LineUnavailableException, InterruptedException {
        final AudioFormat af = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);
        SourceDataLine line = AudioSystem.getSourceDataLine(af);
        line.open(af, SAMPLE_RATE);
        line.start();
        long startTime = System.nanoTime();
        double currentTime = 0;
        Random random = new Random();
        while (currentTime < allDurationMS) {
            byte [] toneBuffer = createSinWaveBuffer(soundFrequency, soundDurationMs);
            line.write(toneBuffer, 0, toneBuffer.length);
            int nextSpaceMs = random.nextInt(randomSpaceTo - randomSpaceFrom) + randomSpaceFrom;
            TimeUnit.MILLISECONDS.sleep(nextSpaceMs);
            currentTime = (System.nanoTime() - startTime)/1000_000.0;
        }
        line.drain();
        line.close();
    }

}

