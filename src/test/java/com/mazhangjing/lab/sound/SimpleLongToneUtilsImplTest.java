package com.mazhangjing.lab.sound;

import javax.sound.sampled.LineUnavailableException;

public class SimpleLongToneUtilsImplTest {

    @org.junit.Before
    public void setUp() throws Exception {

    }

    @org.junit.Test
    public void playForDuration() throws LineUnavailableException {
        new SimpleLongToneUtilsImpl()
                .playForDuration(450,
                30, 15000, 600);
    }

    @org.junit.Test
    public void playForDuration1() throws LineUnavailableException {
        new SimpleLongToneUtilsImpl()
                .playForDuration(950,
                        1500, 8000, 0,3000);
    }
}