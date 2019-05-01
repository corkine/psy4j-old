package com.mazhangjing.lab.sound;

import org.junit.Test;

import javax.sound.sampled.LineUnavailableException;

public class SimpleShortToneUtilsImplTest {

    @Test
    public void A_playForDuration() throws LineUnavailableException, InterruptedException {
        new SimpleShortToneUtilsImpl().playForDuration(300, 1000, 10000, 0, 1000);
    }

    @Test public void B_playForDuration() throws LineUnavailableException, InterruptedException {
        new SimpleShortToneUtilsImpl().playForDuration(300, 1000, 10000, 1000);
    }

}