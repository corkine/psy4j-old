package com.mazhangjing.rhythm.help;

import com.mazhangjing.lab.sound.AudioFunction;
import com.mazhangjing.lab.sound.AudioMaker;
import com.mazhangjing.lab.sound.SimpleAudioFunctionMakerToneUtilsImpl;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SimpleAudioFunctionMakerToneUtilsImplTest {

    private AudioFunction function;

    private final File file = new File("/Users/corkine/工作文件夹/Psy4J/src/test/junk.wav");

    private final File file2 = new File("/Users/corkine/工作文件夹/Psy4J/src/test/junk2.wav");


    @Before public void test() {
        function = new SimpleAudioFunctionMakerToneUtilsImpl();
    }

    @Test
    public void playSound() throws IOException, UnsupportedAudioFileException, InterruptedException {
        File file = new File("/Users/corkine/工作文件夹/Psy4J/src/test/junk.wav");
        boolean exists = file.exists();
        assertTrue(exists);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        new Thread(() -> function.playSound(audioInputStream)).start();
        TimeUnit.SECONDS.sleep(3);
        System.out.println("In here");
        function.stopPlayMarker_$eq(true);
        TimeUnit.SECONDS.sleep(20);
    }

    @Test public void playSound2() {
        function.playSound(file);
    }

    @Test
    public void recordToFile() throws InterruptedException {
        AudioFormat format = new AudioFormat(41000, 16, 1, true, true);
        new Thread(() -> function.recordToFile(format, AudioFileFormat.Type.WAVE, file2)).start();
        TimeUnit.SECONDS.sleep(5);
        System.out.println("In Here");
        function.stopRecordMarker_$eq(true);
        assertTrue(file2.exists());
        function.playSound(file2);
    }

    @Test
    public void recordToStream() throws InterruptedException {
        AudioFormat format = new AudioFormat(41000, 16, 1, true, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new Thread(() -> function.recordToStream(format, stream)).start();
        TimeUnit.SECONDS.sleep(5);
        System.out.println("In Here");
        function.stopRecordMarker_$eq(true);
        TimeUnit.SECONDS.sleep(10);
        System.out.println("stream = " + stream);
    }

    @Test
    public void makeTone() {
        SimpleAudioFunctionMakerToneUtilsImpl function = (SimpleAudioFunctionMakerToneUtilsImpl) this.function;
        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        byte[] bytes = function.makeTone(format, 10000, 400, 16000);
        AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(bytes), format, bytes.length/format.getFrameSize());
        long start = System.currentTimeMillis();
        function.playSound(stream);
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }

    @Test public void playSound3() {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        function.playSound(((AudioMaker) function),format,10000,300,16000);
    }

    @Test public void playForDuration() {
        SimpleAudioFunctionMakerToneUtilsImpl function = (SimpleAudioFunctionMakerToneUtilsImpl) this.function;
        function.playForDuration(300,1500,10000,1500);
    }

    @Test public void testTimeArray() {
        SimpleAudioFunctionMakerToneUtilsImpl function = (SimpleAudioFunctionMakerToneUtilsImpl) this.function;
        for (int i = 0; i < 10000; i++) {
            int[] timeArray = function.getTimeArray(30, 0, 500, 10000);
            System.out.println("timeArray = " + Arrays.toString(timeArray));
        }
    }

    @Test public void testPlayForDuration() {
        SimpleAudioFunctionMakerToneUtilsImpl function = (SimpleAudioFunctionMakerToneUtilsImpl) this.function;
        function.playForDuration(300,30,10000,0,600);
    }
}