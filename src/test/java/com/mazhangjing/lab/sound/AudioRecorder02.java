package com.mazhangjing.lab.sound;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import javax.sound.sampled.*;

public class AudioRecorder02 extends JFrame{

    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;

    private final JButton captureBtn = new JButton("Capture");
    private final JButton stopBtn = new JButton("Stop");

    private final JRadioButton aifcBtn = new JRadioButton("AIFC");
    private final JRadioButton aiffBtn = new JRadioButton("AIFF");
    private final JRadioButton auBtn = new JRadioButton("AU",true); //selected at startup
    private final JRadioButton sndBtn = new JRadioButton("SND");
    private final JRadioButton waveBtn = new JRadioButton("WAVE");

    public static void main(String[] args){
        new AudioRecorder02();
    }

    public AudioRecorder02(){
        captureBtn.setEnabled(true);
        stopBtn.setEnabled(false);

        //Register anonymous listeners
        //end actionPerformed
        captureBtn.addActionListener(e -> {
            captureBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            captureAudio();
        });

        //end actionPerformed
        stopBtn.addActionListener(e -> {
            captureBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            targetDataLine.stop();
            targetDataLine.close();
        });

        //Put the buttons in the JFrame
        getContentPane().add(captureBtn);
        getContentPane().add(stopBtn);

        //Include the radio buttons in a group
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(aifcBtn);
        btnGroup.add(aiffBtn);
        btnGroup.add(auBtn);
        btnGroup.add(sndBtn);
        btnGroup.add(waveBtn);

        //Add the radio buttons to the JPanel
        JPanel btnPanel = new JPanel();
        btnPanel.add(aifcBtn);
        btnPanel.add(aiffBtn);
        btnPanel.add(auBtn);
        btnPanel.add(sndBtn);
        btnPanel.add(waveBtn);

        //Put the JPanel in the JFrame
        getContentPane().add(btnPanel);

        //Finish the GUI and make visible
        getContentPane().setLayout(new FlowLayout());
        setTitle("Copyright 2003, R.G.Baldwin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(300,120);
        setVisible(true);
    }

    //This method captures audio input from a
    // microphone and saves it in an audio file.
    private void captureAudio(){
        try{
            //Get things set up for capture
            audioFormat = getAudioFormat();
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);

            new CaptureThread().start();
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    //This method creates and returns an
    // AudioFormat object for a given set of format
    // parameters.  If these parameters don't work
    // well for you, try some of the other
    // allowable parameter values, which are shown
    // in comments following the declarations.
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
        public void run(){
            AudioFileFormat.Type fileType = null;
            File audioFile = null;

            //Set the file type and the file extension
            // based on the selected radio button.
            if(aifcBtn.isSelected()){
                fileType = AudioFileFormat.Type.AIFC;
                audioFile = new File("junk.aifc");
            }else if(aiffBtn.isSelected()){
                fileType = AudioFileFormat.Type.AIFF;
                audioFile = new File("junk.aif");
            }else if(auBtn.isSelected()){
                fileType = AudioFileFormat.Type.AU;
                audioFile = new File("junk.au");
            }else if(sndBtn.isSelected()){
                fileType = AudioFileFormat.Type.SND;
                audioFile = new File("junk.snd");
            }else if(waveBtn.isSelected()){
                fileType = AudioFileFormat.Type.WAVE;
                audioFile = new File("junk.wav");
            }

            try{
                targetDataLine.open(audioFormat);
                targetDataLine.start();
                AudioSystem.write(new AudioInputStream(targetDataLine), fileType, audioFile);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}