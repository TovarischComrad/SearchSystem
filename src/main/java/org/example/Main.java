package org.example;

import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class Main {

    static public JSONArray parseJSONFile(String jsonFilePath) throws IOException {
        InputStream jsonFile = Files.newInputStream(Paths.get(jsonFilePath));
        Reader readerJson = new InputStreamReader(jsonFile);
        Object fileObjects = JSONValue.parse(readerJson);
        return (JSONArray) fileObjects;
    }

    static public void addDocuments(JSONArray jsonObjects, IndexWriter indexWriter) {
        for (JSONObject object : ((List<JSONObject>) jsonObjects)) {
            Document doc = new Document();
            doc.add(new TextField("text", (String) object.get("text"), Field.Store.YES));
            try {
                System.out.println(doc);
                indexWriter.addDocument(doc);
            } catch (IOException ex) {
                System.err.println("Error adding documents to the index. " + ex.getMessage());
            }
        }
    }

    static public void main(String[] args) throws IOException, ParseException {

        // Инициализация словаря наиболее распространённых грамматических ошибок
        HashMap<String, String> morph = new HashMap<>();
        morph.put("а", "о");
        morph.put("и", "е");
        morph.put("п", "б");
        morph.put("с", "з");
        morph.put("о", "ё");
        morph.put("ы", "и");
        morph.put("ю", "у");

        RussianAnalyzer analyzer = new RussianAnalyzer();
        Directory index = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);

        JSONArray arr = parseJSONFile("article.json");
        addDocuments(arr, w);
        w.close();

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String text = console.readLine();
        String orig = text;
        int k = 0; // итерация поиска
        int t = -1; // указатель на букву слова

        // Сохранение истории поиска
        FileWriter writer = new FileWriter("input2.txt", true);
        writer.write(text);
        writer.append('\n');
        writer.flush();

        // Запрос
        String querystr = args.length > 0 ? args[0] : text;
        Query q = new QueryParser("text", analyzer).parse(querystr);

        // Поиск
        int hitsPerPage = 5;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        Set<String> letters = morph.keySet();
        Iterator<String> iterator = letters.iterator();
        String letter = iterator.next();

        int n = hits.length;

        // Поиск с учётом грамматических ошибок
        while (n == 0 && k < 10) {
            text = orig;
            t = text.indexOf(letter, t + 1);
            if (iterator.hasNext() && t < 0) {
                letter = iterator.next();
                k++;
                continue;
            }
            if (!iterator.hasNext() && t < 0) {
                break;
            }
            text = text.substring(0, t) + morph.get(letter) + text.substring(t + 1);
            querystr = args.length > 0 ? args[0] : text;
            q = new QueryParser("text", analyzer).parse(querystr);
            reader = DirectoryReader.open(index);
            searcher = new IndexSearcher(reader);
            docs = searcher.search(q, hitsPerPage);
            hits = docs.scoreDocs;
            n = hits.length;
            k++;
        }

        // Получение результата запроса
        ArrayList<String> res = new ArrayList<String>();
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            res.add(d.get("text"));
        }
        reader.close();

        // Сохранение результата в файл
        writer = new FileWriter("input1.txt", false);
        for (String re : res) {
            writer.write(re);
            writer.append('\n');
        }
        writer.flush();

        // Запуск скрипта на Python
        Process p = Runtime.getRuntime().exec("python main.py");
    }
}