package com.jinghu.cad.analysis.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class ZipFileUtils {

    /**
     * 解压ZIP
     */
    public static void unzip(String zipPath, String outputDir) throws IOException {
        File zipFile = new File(zipPath);
        URI fileUri = zipFile.toURI();
        URI jarUri = URI.create("jar:" + fileUri);
        Map<String, String> env = new HashMap<>();
        env.put("encoding", "GBK");
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, env)) {
            Path zipRoot = fs.getPath("/");
            Files.walkFileTree(zipRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path dest = Paths.get(outputDir, file.toString());
                    Files.createDirectories(dest.getParent());
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path dest = Paths.get(outputDir, dir.toString());
                    Files.createDirectories(dest);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

}
