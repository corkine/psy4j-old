package com.mazhangjing.lab.sound;

import javax.sound.sampled.LineUnavailableException;

public interface ToneUtils {
    void playForDuration(double soundFrequency, int soundDurationMs, int allDurationMS, int spaceMS) throws InterruptedException, LineUnavailableException;
    void playForDuration(double soundFrequency, int soundDurationMs,
                         int allDurationMS, int randomSpaceFrom, int randomSpaceTo) throws LineUnavailableException, InterruptedException;
}
