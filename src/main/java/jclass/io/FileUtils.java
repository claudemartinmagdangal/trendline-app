package jclass.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class FileUtils {
    public static void writeDataOnFile(Path fileNameWithPath, String data) throws IOException {
        RandomAccessFile stream = new RandomAccessFile(fileNameWithPath.toString(), "rw");
        FileChannel channel = stream.getChannel();
        if (data != null) {
            byte[] strBytes = data.getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(strBytes.length);
            buffer.put(strBytes);
            buffer.flip();
            channel.write(buffer);

            stream.close();
            channel.close();

        }
    }

    public static List<String> readDataAll(Path path) {
        try {
            return Files.readAllLines(path).size() != 0 ? Files.readAllLines(path) : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static String readData(Path path) {
        //System.out.println(" Symbol : "+path.getFileName());
        try {
            return Files.readAllLines(path).size() != 0 ? Files.readAllLines(path).get(0) : "";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> readFile() {
        List<String> list = new LinkedList<String>();

        try {
            File file = new File("RowMoved.txt");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();
            String line;

            while ((line = br.readLine()) != null) {
                list.add(line);
            }
            fr.close();
            //System.out.println("Contents of File: ");
            //System.out.println(sb);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void writeFile(List<String> list) {
        try {
            File fout = new File("RowMoved.txt");
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            for (int i = 0; i < list.size(); i++) {
                bw.write(list.get(i));
                bw.newLine();
            }

            bw.close();

        }catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static int getSymboleIndex(){

        List<String> list=readFile();
        int index=1;
        for(int i=0;i<list.size();i++){
            if(list.get(i).equals("Symbol")) {
                index = i;
                break;
            }
        }
        return index;
    }
}
