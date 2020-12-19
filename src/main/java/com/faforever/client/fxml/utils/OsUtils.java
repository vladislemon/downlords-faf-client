package com.faforever.client.fxml.utils;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class OsUtils {

    public static String[] GetDirectoryFiles(Path path, boolean recursive, Predicate<File> isValidFile) {

        ArrayList<String> resultList = new ArrayList<>();
        if (path == null) {
            path = Path.of(".");
        }

        if (!recursive) {
            File dir = path.toFile();
            if (dir.isDirectory()) {
                File[] contents = dir.listFiles();
                for (File itemFile : contents) {

                    if (!isValidFile.test(itemFile)) {
                        continue;
                    }
                    resultList.add(itemFile.getPath());
                }
            } else {
                resultList.add(dir.getPath());
            }
            return GetStringsOfArray(resultList);
        }

        try {
            Files.walk(path).forEach(filePath -> {

                if (Files.isRegularFile(filePath)) {
                    if (!isValidFile.test(filePath.toFile())) {
                        return;
                    }
                    String filePathName = filePath.toString();
                    resultList.add(filePathName);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return GetStringsOfArray(resultList);
    }

    private static String[] GetStringsOfArray(List<String> resultList) {
        String[] result = new String[resultList.size()];

        for (int i = 0; i < resultList.size(); i++) {
            String item = resultList.get(i);
            result[i] = item;
        }
        return result;
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static Document readXmlPlain(String fileName) {
        Document dom = null;
        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            File file = new File(fileName);
            dom = db.parse(file);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return dom;
    }

    public static void writeAllText(Path pathName, String generateCode) {
        try (PrintWriter out = new PrintWriter(pathName.toAbsolutePath().toString())) {
            out.print(generateCode);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static List<String> readAllLines(String file) {
        try {
            return Files.readAllLines(Paths.get(file));
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
}
