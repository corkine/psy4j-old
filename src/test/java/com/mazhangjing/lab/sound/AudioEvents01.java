package com.mazhangjing.lab.sound;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AudioEvents01 extends JFrame{

    private boolean stopCapture = false;
    private ByteArrayOutputStream byteArrayOutputStream;
    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;

    public static void main(String[] args){
        new AudioEvents01();
    }

    public AudioEvents01(){
        final JButton captureBtn = new JButton("Capture");
        final JButton stopBtn = new JButton("Stop");
        final JButton playBtn = new JButton("Playback");

        captureBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        playBtn.setEnabled(false);

        //Register anonymous listeners
        captureBtn.addActionListener(e -> {
            captureBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            playBtn.setEnabled(false);
            captureAudio();
        });
        getContentPane().add(captureBtn);

        //end actionPerformed
        stopBtn.addActionListener(e -> {
            captureBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            playBtn.setEnabled(true);
            stopCapture = true;
        });
        getContentPane().add(stopBtn);

        playBtn.addActionListener(e -> playAudio());
        getContentPane().add(playBtn);

        getContentPane().setLayout(new FlowLayout());
        setTitle("Copyright 2003, R.G.Baldwin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(250,70);
        setVisible(true);
    }

    private void captureAudio(){
        try{
            //Get everything set up for capture
            audioFormat = getAudioFormat();
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine)AudioSystem.getLine(dataLineInfo);

            targetDataLine.addLineListener(e -> {
                        System.out.println("Event handler for TargetDataLine");
                        System.out.println("Event type: " + e.getType());
                        System.out.println("Line info: " + e.getLine().getLineInfo());
                        long framePosition = e.getFramePosition();
                        System.out.println("framePosition = " + framePosition);
                        System.out.println();
                    }
            );
            new CaptureThread().start();
        }catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    private void playAudio() {
        try{
            byte[] audioData = byteArrayOutputStream.toByteArray();
            InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
            AudioFormat audioFormat = getAudioFormat();
            audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length/audioFormat.getFrameSize());
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
            sourceDataLine.addLineListener(e -> {
                        System.out.println("Event handler for SourceDataLine");
                        System.out.println("Event type: " + e.getType());
                        System.out.println("Line info: " + e.getLine().getLineInfo());
                        long framePosition = e.getFramePosition();
                        System.out.println("framePosition = " + framePosition);
                        System.out.println();//blank line
                    }
            );
            new PlayThread().start();
        }catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    private AudioFormat getAudioFormat(){
        float sampleRate = 8000.0F;
        //8000,11025,16000,22050,44100
        int sampleSizeInBits = 16;
        //8,16
        int channels = 1;
        //1,2
        boolean signed = true;
        //true,false
        boolean bigEndian = false;
        //true,false
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    class CaptureThread extends Thread{
        byte[] tempBuffer = new byte[10000];
        public void run(){
            byteArrayOutputStream = new ByteArrayOutputStream();
            stopCapture = false;
            try{
                targetDataLine.open(audioFormat);
                targetDataLine.start();
                while(!stopCapture){
                    int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                    if(cnt > 0){
                        byteArrayOutputStream.write(tempBuffer, 0, cnt);
                    }
                }
                byteArrayOutputStream.close();
                targetDataLine.stop();
                targetDataLine.close();
            }catch (Exception e) {
                e.printStackTrace(System.out);
                System.exit(0);
            }
        }
    }

    class PlayThread extends Thread{
        byte[] tempBuffer = new byte[10000];

        public void run(){
            try{
                int cnt;
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();
                while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
                    if(cnt > 0){
                        sourceDataLine.write(tempBuffer, 0, cnt);
                    }
                }
                sourceDataLine.drain();
                sourceDataLine.close();
            }catch (Exception e) {
                e.printStackTrace(System.err);
                System.exit(0);
            }
        }
    }
}