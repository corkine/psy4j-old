package com.mazhangjing.lab.sound;


import javax.sound.sampled.*;
import javax.swing.*;
import java.io.File;

public class AudioPlayer02 extends JFrame{

    private AudioFormat audioFormat;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;
    private boolean stopPlayback = false;
    private final JButton stopBtn = new JButton("Stop");
    private final JButton playBtn = new JButton("Play");
    private final JTextField textField = new JTextField("junk.au");

    public static void main(String[] args){
        new AudioPlayer02();
    }

    public AudioPlayer02(){
        stopBtn.setEnabled(false);
        playBtn.setEnabled(true);

        //end actionPerformed
        playBtn.addActionListener(e -> {
            stopBtn.setEnabled(true);
            playBtn.setEnabled(false);
            playAudio();
        });

        //end actionPerformed
        stopBtn.addActionListener(e -> stopPlayback = true);

        getContentPane().add(playBtn,"West");
        getContentPane().add(stopBtn,"East");
        getContentPane().add(textField,"North");

        setTitle("Copyright 2003, R.G.Baldwin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(250,70);
        setVisible(true);
    }

    private void playAudio() {
        try{
            File soundFile = new File(textField.getText());
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            audioFormat = audioInputStream.getFormat();
            System.out.println(audioFormat);

            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

            sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);

            new PlayThread().start();
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    class PlayThread extends Thread{
        byte[] tempBuffer = new byte[10000];

        public void run(){
            try{
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();

                int cnt;
                while((cnt = audioInputStream.read(tempBuffer,0,tempBuffer.length)) != -1 && !stopPlayback){
                    if(cnt > 0) sourceDataLine.write(tempBuffer, 0, cnt);
                }
                sourceDataLine.drain();
                sourceDataLine.close();

                //Prepare to playback another file
                stopBtn.setEnabled(false);
                playBtn.setEnabled(true);
                stopPlayback = false;
            }catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}