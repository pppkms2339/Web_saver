package com.company.server;

import java.io.File;

public class FileHandler {
    private static StringBuilder structureString;

    public static String buildFolderStructure(File dir) {
        structureString = new StringBuilder("<");
        build(dir);
        String answer = structureString.toString();
        return answer;
    }

    private static void build(File f) {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            structureString.append(files[i].getName());
            if (files[i].isDirectory()) {
                structureString.append("<");
                build(files[i]);
            }
            structureString.append("|");
        }
        if (files.length != 0) {
            int n = structureString.length();
            structureString.delete(n - 1, n);
        }
        structureString.append(">");
    }

    //Рекурсивное удаление папок (т.к. для удаления папка должна быть пустой)
    public static void delete(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            for (File f : file.listFiles())
                delete(f);
        }
        file.delete();
    }

    //Прибавляем к имени файла счетчик до тех пор, пока имя файла не будет уникальным
    public static String newFileName(String oldName) {
        File f = null;
        int count = 0;
        String[] fName = oldName.split("\\.");
        String answer, baseName = fName[fName.length - 2];
        do {
            count++;
            answer = baseName + "(" + count + ")." + fName[fName.length - 1];
            f = new File(answer);
        } while (f.exists());
        return answer;
    }
}
