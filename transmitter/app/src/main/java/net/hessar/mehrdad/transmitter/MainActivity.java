package net.hessar.mehrdad.transmitter;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    static final short zeroValue = 1638;            //5% of short maximum value
    static final short oneValue  = 31129;           //95% of short maximum value
    static final double aFactor   = 230.3984;
    static final double bFactor   = 1638;

    static final int sl = 100;
    static final int frequency  = 10000;
    static final float increment = (float)(2*Math.PI) * frequency / 44100; // angular increment for each sample

    private int bufferSize;
    private AudioTrack audioTrack;
    private short [] buffer;

    private EditText editText;
    private Button sendB;
    private Button stopB;
    private Boolean isSending;
;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        bufferSize = 10000;

        //Create message textbox object
        editText = (EditText) findViewById(R.id.message);
        editText.setText("abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ !@#$%^&*()_+ZXCVB");
        System.out.println("Length : " + editText.getText().toString().length());

        //Initialize audio track objects
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);
//        samples = new float[bufferSize];
//        buffer = new short[bufferSize];


        isSending = new Boolean(false);
        sendB = (Button) findViewById(R.id.sendB);
        sendB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isSending = true;
                new Thread(new Runnable() {
                    public void run() {
                        buffer = createWave();
                        audioTrack.play();
                        System.out.println("Buffer Size: " + buffer.length);
                        audioTrack.write(buffer, 0, buffer.length);  //write to the audio buffer.... and start all over again!

//                        audioTrack.flush();
//                        audioTrack.stop();
                    }
                }).start();
            }
        });

        stopB = (Button) findViewById(R.id.stopB);
        stopB.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                isSending = false;
                String message = editText.getText().toString();
                System.out.println(message);
                createPreamble();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private byte [] readData ()
    {
        String temp = editText.getText().toString();
        return temp.getBytes();
    }

    private short [] createSync ()
    {
        short [] sync = new short[13*sl];
        short [] one  = createOne();
        short [] zero = createZero();

        //First
        for (int j=0; j<sl; j++)
        {
            sync[j*1] = one[j];
        }
        //2nd
        for (int j=0; j<100; j++)
        {
            sync[j+1*sl] = one[j];
        }
        //3nd
        for (int j=0; j<100; j++)
        {
            sync[j+2*sl] = one[j];
        }
        //4nd
        for (int j=0; j<100; j++)
        {
            sync[j+3*sl] = one[j];
        }
        //5nd
        for (int j=0; j<100; j++)
        {
            sync[j+4*sl] = one[j];
        }

        //6nd
        for (int j=0; j<100; j++)
        {
            sync[j+5*sl] = zero[j];
        }
        //7nd
        for (int j=0; j<100; j++)
        {
            sync[j+6*sl] = zero[j];
        }

        //8nd
        for (int j=0; j<100; j++)
        {
            sync[j+7*sl] = one[j];
        }
        //9nd
        for (int j=0; j<100; j++)
        {
            sync[j+8*sl] = one[j];
        }

        //10nd
        for (int j=0; j<100; j++)
        {
            sync[j+9*sl] = zero[j];
        }
        //11nd
        for (int j=0; j<100; j++)
        {
            sync[j+10*sl] = one[j];
        }
        //12nd
        for (int j=0; j<100; j++)
        {
            sync[j+11*sl] = zero[j];
        }
        //13nd
        for (int j=0; j<100; j++)
        {
            sync[j+12*sl] = one[j];
        }

        return sync;
    }

    short [] createOne ()
    {
        float time = 0;
        short [] result = new short[100];
        for (int i=0; i<100; i++)
        {
            result[i] = (short) (((float) Math.sin(time)) * Short.MAX_VALUE);
            time += increment;
        }
        return result;
    }

    short [] createZero ()
    {
        short [] result = new short[100];
        for (int i=0; i<100; i++)
        {
            result[i] = 0;
        }
        return result;
    }

    private short [] createWave ()
    {
        short [] sync = createSync();
        short [] preamble = createPreamble();
        String message = editText.getText().toString();
        System.out.println(message);

        byte [] binary = message.getBytes();
        System.out.println("Message String size: " + message.length());
        String binaryMessage = new String("");

        for (int i=0; i<binary.length; i++)
        {
            binaryMessage += String.format("%8s", Integer.toBinaryString((int) binary[i])).replace(' ', '0');
        }
        System.out.println("Message Length:" + binaryMessage.length());

        short [] wave = new short[binaryMessage.length()*sl + sync.length + preamble.length];
        short [] one  = createOne();
        short [] zero = createZero();

        //Add sync to wave
        System.arraycopy(sync, 0, wave, 0, sync.length);
        //Add preamble to wave
        System.arraycopy(preamble, 0, wave, sync.length, preamble.length);
        //Add data to wave
        for (int j=0; j<binaryMessage.length(); j++)
        {
            if (binaryMessage.charAt(j) == '0')
                System.arraycopy(zero, 0, wave, sync.length + preamble.length + j*sl, zero.length);
            else
                System.arraycopy(one, 0,  wave, sync.length + preamble.length + j*sl, one.length);
        }
        System.out.println("Message wave Length:" + wave.length);
        return wave;
    }

    private short [] createPreamble()
    {
        short [] result = new short[13*sl];
        short [] one = createOne();

        for (int i=0; i<13; i++)
            System.arraycopy(one, 0, result, i*sl, one.length);

        return result;
    }

    private void writeWaveToFile (String fileName, short [] samples)
    {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(dir + File.separator + fileName);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter buf = new BufferedWriter(new FileWriter(file, false));

            for (int i=0; i<samples.length; i++){
                buf.append("" + samples[i]);
                buf.newLine();
            }

            buf.flush();
            buf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}