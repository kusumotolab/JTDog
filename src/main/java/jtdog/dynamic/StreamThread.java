package jtdog.dynamic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class StreamThread extends Thread {
    private InputStream in;
    private String type;

    public StreamThread(InputStream in, String type) {
        this.in = in;
        this.type = type;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "MS932"))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                // ログなど出力する
                //System.out.println(type + ">" + line);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}