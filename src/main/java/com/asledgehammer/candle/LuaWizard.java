package com.asledgehammer.candle;

import com.asledgehammer.candle.impl.KnownTypeRenderer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LuaWizard {

    public static void main(String[] yargs) throws IOException {

        String path = "./dist/";
        if (yargs.length != 0) path = yargs[1];

        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Failed to mkdirs: " + path);

        Candle candle = new Candle();
        candle.walk(true);

        KnownTypeRenderer renderer = new KnownTypeRenderer();
        candle.render(renderer);
//        candle.save(dir);

        FileWriter fw = new FileWriter(new File(dir, "known_types.json"));
        fw.write("{\n" + renderer.file + "}\n");
        fw.flush();
        fw.close();


    }
}
