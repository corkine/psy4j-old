package com.mazhangjing.lab.sound;

import org.junit.Test;

import javax.sound.sampled.LineUnavailableException;

public class WaveUtilTest {

    @Test
    public void A_playForDuration() throws LineUnavailableException, InterruptedException {
        WaveUtil.playForDuration(1000, 20, 10000, 0, 1000);
    }

    @Test public void B_playForDuration() throws LineUnavailableException, InterruptedException {
        WaveUtil.playForDuration(1000, 20, 10000, 1000);
    }

}