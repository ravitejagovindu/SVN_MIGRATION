/**
 *
 */
package com.ofss.fileComparison.startup;

import com.ofss.fileComparison.dao.Dao;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author rtgovind
 *
 */
public class FileComparisonApp {

    private static String filesToBeDeployedDirectoryPath;
    private static String destinationFolder;

    public static void main(String[] args) {

        try {
            String oldDirectoryPath = args[0];
            String newDirectoryPath = args[1];
            filesToBeDeployedDirectoryPath = args[2];
            destinationFolder = filesToBeDeployedDirectoryPath + "\\Deployables\\";
            compareFiles(oldDirectoryPath, newDirectoryPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void compareFiles(String oldDirectoryPath, String newDirectoryPath) throws IOException {

        Map<String, Map<String, File>> oldFilesByDirectory = getFiles(new File(oldDirectoryPath));
        Map<String, Map<String, File>> newFilesByDirectory = getFiles(new File(newDirectoryPath));
        Set<File> filesToBeDeployed = new HashSet<>();

        newFilesByDirectory.forEach((dir, newFileSet) -> {
            Map<String, File> oldFileSet = oldFilesByDirectory.getOrDefault(dir, new HashMap<>());
            filesToBeDeployed.addAll(compare(oldFileSet, newFileSet, dir));
        });

        if (!filesToBeDeployed.isEmpty()) {
            String dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date());
            PrintWriter writer = new PrintWriter(
                    new File(filesToBeDeployedDirectoryPath + "\\filesToBeDeployed_" + dateFormat + ".txt"));
            filesToBeDeployed.forEach(file -> {
                writer.println("copy " + file + " " + destinationFolder + file.getName());
            });
            writer.close();
        }

    }

    private static Map<String, Map<String, File>> getFiles(File directory) {
        Map<String, Map<String, File>> filesByDirectory = new HashMap<>();
        Set<File> directories = Stream.of(directory.listFiles()).collect(Collectors.toSet());

        directories.stream().forEach(dir -> {
            Map<String, File> fileMap = new HashMap<>();

            if (dir.getName().equals("UIXML")) {
                Set<File> languageDirectories = Stream.of(dir.listFiles()).collect(Collectors.toSet());
                languageDirectories.stream().forEach(langDir -> {
                    Stream.of(langDir.listFiles()).collect(Collectors.toSet()).forEach(file -> {
                        fileMap.put(file.getName(), file);
                    });
                });
            } else if (dir.isDirectory()) {
                Stream.of(dir.listFiles()).collect(Collectors.toSet()).forEach(file -> {
                    fileMap.put(file.getName(), file);
                });
            } else {
                fileMap.put(dir.getName(), dir);
            }
            filesByDirectory.put(dir.getName(), fileMap);
        });

        return filesByDirectory;
    }

    private static Set<File> compare(Map<String, File> oldFileSet, Map<String, File> newFileSet, String dir) {

        Set<File> filesToBeDeployed = new HashSet<>();
        newFileSet.forEach((fileName, newFile) -> {
            File oldFile = oldFileSet.get(fileName);
            try {
                if (oldFile != null) {
                    if (!areEqual(newFile, oldFile)) {
                        filesToBeDeployed.add(newFile);
                        if (oldFile.getName().indexOf(".INC") != -1)
                            copyChangedQueriesToNewFile(newFile, oldFile);
                    }
                } else
                    filesToBeDeployed.add(newFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return filesToBeDeployed;
    }

    private static boolean areEqual(File newFile, File oldFile) throws IOException {

        boolean areEqual = true;
        BufferedReader oldFilesReader = new BufferedReader(new FileReader(oldFile));
        BufferedReader newFilesReader = new BufferedReader(new FileReader(newFile));
        String oldLine = oldFilesReader.readLine();
        String newLine = newFilesReader.readLine();

        while (oldLine != null || newLine != null) {

            if ((oldLine == null && newLine != null) || (oldLine != null && newLine == null)
                    || (!oldLine.equalsIgnoreCase(newLine))) {
                areEqual = false;
                break;
            }
            oldLine = oldFilesReader.readLine();
            newLine = newFilesReader.readLine();
        }
        oldFilesReader.close();
        newFilesReader.close();

        return areEqual;
    }

    private static void copyChangedQueriesToNewFile(File newFile, File oldFile) {
        Set<String> oldFileAsSet = getFileAsSet(oldFile);
        Set<String> newFileAsSet = getFileAsSet(newFile);
        Set<String> inserts = new HashSet<>();
        if (oldFileAsSet.containsAll(newFileAsSet))
            return;
        StringBuffer sb = new StringBuffer();
        String newFileName = newFile.getName();
        String changedINCFilesPath = filesToBeDeployedDirectoryPath + "\\CHANGED_INC_FILES";
        File changedINCFile = new File(changedINCFilesPath + "\\" + newFileName);
        String tableName = newFileName.substring(0, newFileName.indexOf("__"));

        newFileAsSet.forEach(line -> {
            if (!oldFileAsSet.contains(line) && !line.startsWith("DELETE")) {
                inserts.add(line);
            }
        });
        if (!inserts.isEmpty())
            writeToFile(changedINCFilesPath, changedINCFile, inserts, getDeletes(inserts, tableName, newFile));
    }

    private static Set<String> getDeletes(Set<String> inserts, String tableName, File newFile) {

        Set<String> deletes = new HashSet<>();
        Set<String> primaryColumns = getPrimaryColumns(tableName);
        Map<String, String> columnToValueMap = new HashMap<>();
        inserts.forEach(insert -> {
            StringBuffer deleteStatement = new StringBuffer("DELETE FROM " + tableName + " WHERE ");
            String[] columns = getStringParts(insert, "(", ")", ",");
            String[] values = getStringParts(insert, "VALUES(", ");", newFile.getName().contains("_LOV_") ? "','" : ",");
            if (columns.length != values.length) {
                if (insert.contains("'',''")) {
                    insert = insert.replace("'',''", "\"\"\"");
                    columns = getStringParts(insert, "(", ")", ",");
                    values = getStringParts(insert, "VALUES(", ");", newFile.getName().contains("_LOV_") ? "','" : ",");
                    if (columns.length != values.length) {
                        throw new RuntimeException("Unable to get the values of columns even after replacing delimiter.\nFile name ::  "
                                + newFile.getAbsolutePath() + "\nInsert Statement :: " + insert + "\nDelimiter used :: ',' ");
                    }
                } else {
                    throw new RuntimeException("Unable to get the values of columns.\nFile name ::  "
                            + newFile.getAbsolutePath() + "\nInsert Statement :: " + insert + "\nDelimiter used :: ',' ");
                }
            }
            for (int i = 0; i < columns.length; i++) {
                if (values[i].contains("\"\"\"")) {
                    values[i] = values[i].replace("\"\"\"", "'',''");
                }
                if (!values[i].startsWith("'")) {
                    values[i] = "'" + values[i];
                }
                if (!values[i].endsWith("'")) {
                    values[i] = values[i] + "'";
                }
                columnToValueMap.put(columns[i], values[i]);
            }
            primaryColumns.forEach(column -> {
                deleteStatement.append(column).append(" = ").append(columnToValueMap.get(column)).append(" AND ");
            });
            deletes.add(deleteStatement.delete(deleteStatement.lastIndexOf(" AND "), deleteStatement.length()).append(";").toString());
        });

        return deletes;
    }

    private static String[] getStringParts(String insert, String start, String end, String regex) {
        return insert.substring(insert.indexOf(start) + start.length(), insert.indexOf(end)).split(regex);
    }

    private static Set<String> getPrimaryColumns(String tableName) {
        return new Dao().getPrimaryColumns(tableName);
    }

    private static void writeToFile(String changedINCFilesPath, File changedINCFile, Set<String> inserts, Set<String> deletes) {
        if (!new File(changedINCFilesPath).exists()) {
            boolean directoryCreated = (new File(changedINCFilesPath)).mkdir();
            if (!directoryCreated)
                throw new RuntimeException("Changed INC Files Directory creation failure.");
        }
        String dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date());
        if (changedINCFile.exists()) {
            changedINCFile.renameTo(new File(changedINCFile.getAbsolutePath() + "_" + dateFormat + ".INC"));
        }
        try {
            PrintWriter writer = new PrintWriter(changedINCFile);
            System.out.println("getChangedQueriesFile() :: writing");
            deletes.forEach(delete -> {
                System.out.println(delete);
                writer.println(delete);
            });
            inserts.forEach(insert -> {
                System.out.println(insert);
                writer.println(insert);
            });
            writer.println("COMMIT;");
            System.out.println("getChangedQueriesFile() :: written");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Set<String> getFileAsSet(File file) {
        Set<String> fileAsSet = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            String temp = "";
            while ((line = reader.readLine()) != null) {
                if (!line.endsWith(");")) {
                    temp = temp + line;
                    continue;
                } else if (!temp.isEmpty()) {
                    temp = temp + line;
                }
                if (temp.isEmpty()) {
                    fileAsSet.add(line);
                } else {
                    fileAsSet.add(temp);
                }
                temp = "";
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileAsSet;
    }

}
