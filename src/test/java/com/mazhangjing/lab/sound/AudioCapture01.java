package com.mazhangjing.lab.sound;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AudioCapture01 extends JFrame{

    private boolean stopCapture = false;
    private ByteArrayOutputStream byteArrayOutputStream;
    private TargetDataLine targetDataLine;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;

    public static void main(String[] args){ new AudioCapture01(); }

    public AudioCapture01(){
        final JButton captureBtn = new JButton("Capture");
        final JButton stopBtn = new JButton("Stop");
        final JButton playBtn = new JButton("Playback");

        captureBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        playBtn.setEnabled(false);

        captureBtn.addActionListener(e -> {
            captureBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            playBtn.setEnabled(false);
            captureAudio(); }
        );
        getContentPane().add(captureBtn);

        //end actionPerformed
        stopBtn.addActionListener(e -> {
            captureBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            playBtn.setEnabled(true);
            stopCapture = true;
        }
        );
        getContentPane().add(stopBtn);

        //end actionPerformed
        playBtn.addActionListener(e -> playAudio());
        getContentPane().add(playBtn);

        getContentPane().setLayout(new FlowLayout());
        setTitle("Capture/Playback Demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(350,70);
        setVisible(true);
    }

    //This method captures audio input from a microphone and saves it in a ByteArrayOutputStream object.
    private void captureAudio(){
        try{
            //Get everything set up for capture
            AudioFormat audioFormat = getAudioFormat();
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

            Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
            Mixer mixer = AudioSystem.getMixer(mixerInfo[0]);
            targetDataLine = ((TargetDataLine) mixer.getLine(dataLineInfo));
            //targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            targetDataLine.open();
            targetDataLine.start();

            //Create a thread to capture the microphone data and start it running.  It will run until
            //the Stop button is clicked.
            Thread captureThread = new Thread(new CaptureThread());
            captureThread.start();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    //This method plays back the audio data that has been saved in the ByteArrayOutputStream
    private void playAudio() {
        try{
            //Get everything set up for playback.
            //Get the previously-saved data into a byte array object.
            byte[] audioData = byteArrayOutputStream.toByteArray();
            //Get an input stream on the byte array containing the data
            InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
            AudioFormat audioFormat = getAudioFormat();
            audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat,
                            audioData.length/audioFormat.getFrameSize());
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            //Create a thread to play back the data and start it running.  It will run until
            //all the data has been played back.
            Thread playThread = new Thread(new PlayThread());
            playThread.start();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    //This method creates and returns an AudioFormat object for a given set of format parameters.  If these
    // parameters don't work well for you, try some of the other allowable parameter values, which
    // are shown in comments following the declarations.
    private AudioFormat getAudioFormat(){
        float sampleRate = 44100;
        //8000,11025,16000,22050,44100
        int sampleSizeInBits = 16;
        //8,16
        int channels = 1;
        //1,2
        boolean signed = true;
        //true,false
        boolean bigEndian = true;
        //true,false
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    class CaptureThread extends Thread{
        //An arbitrary-size temporary holding buffer
        byte[] tempBuffer = new byte[10000];
        public void run(){
            byteArrayOutputStream = new ByteArrayOutputStream();
            stopCapture = false;
            try{
                while(!stopCapture){
                    //Read data from the internal buffer of the data line.
                    int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                    if(cnt > 0){
                        //Save data in output stream object.
                        byteArrayOutputStream.write(tempBuffer, 0, cnt);
                    }
                }
                byteArrayOutputStream.close();
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
                //Keep looping until the input read method returns -1 for empty stream.
                while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
                    if(cnt > 0){
                        //Write data to the internal buffer of the data line where it will be delivered to the speaker.
                        sourceDataLine.write(tempBuffer, 0, cnt);
                    }
                }
                //Block and wait for internal buffer of the data line to empty.
                sourceDataLine.drain();
                sourceDataLine.close();
            }catch (Exception e) {
                e.printStackTrace(System.out);
                System.exit(0);
            }
        }
    }
}