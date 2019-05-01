package com.mazhangjing.lab.sound;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 纯音生成工具的简单实现类
 * @implNote 2019-04-23 撰写此类
 */
public class SimpleLongToneUtilsImpl implements ToneUtils {

    private float sampleRate = 16000.0F;
    //Allowable 8000,11025,16000,22050,44100
    private int sampleSizeInBits = 16;
    //Allowable 8,16
    private int channels = 1;
    //Allowable 1,2
    private boolean signed = true;
    private boolean bigEndian = true;

    private AudioFormat audioFormat =  //Get the required audio format
            new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);

    //Get info on the required data line
    private DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

    private SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

    public SimpleLongToneUtilsImpl() throws LineUnavailableException {
    }

    //一种缓冲器，用于以每秒16000采样量存储16位样本的两秒单耳和一秒立体声数据
    //控制时间
    private byte[] getBuffer(double soundMs) {
        return new byte[Math.toIntExact(Math.round(32 * soundMs))];
    }

    private void putSyntheticData(byte[] synDataBuffer, double freq){
        ByteBuffer byteBuffer;
        ShortBuffer shortBuffer;
        int byteLength;
        //Prepare the ByteBuffer and the shortBuffer
        // for use
        byteBuffer = ByteBuffer.wrap(synDataBuffer);
        shortBuffer = byteBuffer.asShortBuffer();

        byteLength = synDataBuffer.length;

        channels = 1;//Java allows 1 or 2
        //Each channel requires two 8-bit bytes per
        // 16-bit sample.
        int bytesPerSamp = 2;
        sampleRate = 16000.0F;
        // Allowable 8000,11025,16000,22050,44100
        int sampLength = byteLength/bytesPerSamp;
        for(int cnt = 0; cnt < sampLength; cnt++){
            double time = cnt/sampleRate;
            double sinValue = (Math.sin(2*Math.PI*freq*time));
            shortBuffer.put((short)(16000*sinValue));
        }
    }

    private synchronized void justPlay(byte[] audioData, int allDurationMS, int spaceMS,
                                       int randomSpaceFrom, int randomSpaceTo)
            throws LineUnavailableException, InterruptedException {
        //Open and start the SourceDataLine
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();
        Random random = new Random();
        long start = System.nanoTime()/1000000;
        while((System.nanoTime()/1000000 - start) < allDurationMS) {
            sourceDataLine.write(audioData, 0, audioData.length);
            sourceDataLine.drain();
            if (spaceMS == -1) {
                int nextSpaceMs = random.nextInt(randomSpaceTo - randomSpaceFrom) + randomSpaceFrom;
                TimeUnit.MILLISECONDS.sleep(nextSpaceMs);
            } else {
                TimeUnit.MILLISECONDS.sleep(spaceMS);
            }
        }
        //Block and wait for internal buffer of the
        // SourceDataLine to become empty.
        sourceDataLine.drain();
        sourceDataLine.stop();
        sourceDataLine.close();
    }

    @Override
    public void playForDuration(double soundFrequency, int soundDurationMs, int allDurationMS, int spaceMS) {
        byte[] buffer = this.getBuffer(soundDurationMs);
        this.putSyntheticData(buffer, soundFrequency);
        //byte[] sinWaveBuffer = createSinWaveBuffer(soundFrequency, soundDurationMs);
        try {
            this.justPlay(buffer, allDurationMS, spaceMS, 0,0);
        } catch (LineUnavailableException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playForDuration(double soundFrequency, int soundDurationMs, int allDurationMS,
                                int randomSpaceFrom, int randomSpaceTo) {
        byte[] buffer = this.getBuffer(soundDurationMs);
        this.putSyntheticData(buffer, soundFrequency);
        try {
            this.justPlay(buffer, allDurationMS, -1, randomSpaceFrom, randomSpaceTo);
        } catch (LineUnavailableException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
