package com.pranish.toolmanager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ToolManager {
    private static final Path TOOLDIR = Path.of(System.getProperty("user.home"),".ppce","tools");
    private static String os()
    {
        String name = System.getProperty("os.name").toLowerCase();
        if(name.contains("win")) return "windows";
        if(name.contains("linux")) return "linux";
        if(name.contains("mac")) return "mac";
        return "unknown";
    }

    public static Path get(String name) throws IOException{
        String os = os();
        String osFolder = os.equals("windows") ? "Windows" : os;
        String folder = name.toUpperCase();
        String file = os.equals("windows") ? name + ".exe": name;

        String resources = "/tools/" + osFolder + "/" + folder + "/" + file;
        Path targetDir = TOOLDIR.resolve(file);
        Path target = targetDir.resolve(file);

        byte[] bytes;
        try(InputStream is = ToolManager.class.getResourceAsStream(resources)){
            if (is == null )
            {
                throw new FileNotFoundException("Tools not bundled: " + resources);
            }
            bytes = is.readAllBytes();
        }
        Files.createDirectories(targetDir);
        if (!Files.exists(target) || !Arrays.equals(Files.readAllBytes(target), bytes)) {
            Files.write(target, bytes);
        }
        target.toFile().setExecutable(true);
        return target;
    }
}
