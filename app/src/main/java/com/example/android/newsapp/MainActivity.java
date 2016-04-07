package com.example.android.newsapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    JSONArray array;
    ListView newsList;
    ArrayList<Integer> newsId = new ArrayList<Integer>();
    Map<Integer,String> newsUrls = new HashMap<Integer,String>();
    Map<Integer,String> newsTitles = new HashMap<Integer,String>();
    ArrayList<String> listViewTiles = new ArrayList<String>();
    ArrayList<String> webViewUrl = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    //DownloadNews downloadNews;
    SQLiteDatabase newsDb;
   // WebView webView;

    public class DownloadNews extends AsyncTask<String,Void,String>{


        @Override
        protected String doInBackground(String... params) {
            URL url;
            String s = "";
            HttpURLConnection httpURLConnection = null;
            try {
                url = new URL(params[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;
                    s += current;
                    data = reader.read();
                }
                //Log.i("Result",result);
                //return result;
                String newsURL = "";
                //JSONObject jsonObject = new JSONObject(s);
                Object obj = new JSONTokener(s).nextValue();
                if (obj instanceof JSONArray) {
                    array = new JSONArray(s);
                    newsDb.execSQL("DELETE FROM News_articles");
                    for (int i = 0; i < 20; i++) {
                        //System.out.println(array.getString(i));
                        DownloadNews newsContent = new DownloadNews();
                        String articleId = array.getString(i);
                       // String content = newsContent.execute("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty").get();
                        url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                        httpURLConnection = (HttpURLConnection)url.openConnection();
                        inputStream = httpURLConnection.getInputStream();
                        reader = new InputStreamReader(inputStream);
                        data = reader.read();
                        String content="";
                        while (data != -1) {
                            char current = (char) data;
                            content += current;
                            data = reader.read();
                        }
                        JSONObject jsonObject = new JSONObject(content);
                        String newsTitle = jsonObject.getString("title");
                        //newsLink.add(newsTitle);
                        if (jsonObject.has("url")) {
                            newsURL = jsonObject.getString("url");
                        } else newsURL = "";
                        newsId.add(Integer.valueOf(articleId));
                        newsUrls.put(Integer.valueOf(articleId), newsURL);
                        newsTitles.put(Integer.valueOf(articleId), newsTitle);
                        String sql = "INSERT INTO News_articles(articleId, url, title) VALUES (? , ?, ? )";
                        SQLiteStatement statement = newsDb.compileStatement(sql);
                        statement.bindString(1, articleId);
                        statement.bindString(2, newsURL);
                        statement.bindString(3, newsTitle);
                        statement.execute();
                    }
                }
            }catch (Exception e) {
                        e.printStackTrace();
                    }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
//            Log.i("Json Data", s);
            super.onPostExecute(s);

//                    Log.i("News Ids",newsId.toString());
//                    Log.i("News Titles",newsTitles.toString());
//                    Log.i("News URL",newsUrls.toString());

                    updateList();
                    newsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Log.i("Fetching URL","URL");
                            Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                            intent.putExtra("articleUrl",webViewUrl.get(position));
                            startActivity(intent);
                            Log.i("Title",listViewTiles.get(position));
                            Log.i("URL", webViewUrl.get(position));
                        }
                    });

                arrayAdapter.notifyDataSetChanged();
                //System.out.println("List data:" +newsLink);
                //ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,newsLink);
               // ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_list_item_1,newsLink);
                //newsList.setAdapter(arrayAdapter);
//                newsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//                    }
//                });

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        newsList = (ListView)findViewById(R.id.listView);
        DownloadNews downloadNews = new DownloadNews();
        newsDb = this.openOrCreateDatabase("News",MODE_PRIVATE,null);

        newsDb.execSQL("CREATE TABLE IF NOT EXISTS News_articles (id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR,title VARCHAR, contents VARCHAR)");
        updateList();
        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,listViewTiles);
        newsList.setAdapter(arrayAdapter);
       // downloadNews.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        try {
            downloadNews.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void updateList(){
        Log.i("Update List","updated");
        Cursor c = newsDb.rawQuery("SELECT * FROM News_articles ORDER BY articleId DESC",null);
        int articleIdIndex = c.getColumnIndex("articleId");
        int newsUrlId = c.getColumnIndex("url");
        int titleId = c.getColumnIndex("title");
        c.moveToFirst();
        listViewTiles.clear();
        webViewUrl.clear();
        while (c != null && !c.isAfterLast()){
            listViewTiles.add(c.getString(titleId));
            webViewUrl.add(c.getString(newsUrlId));
//                        Log.i("Article id",c.getString(articleIdIndex));
//                        Log.i("news Url",c.getString(newsUrlId));
//                        Log.i("Title",c.getString(titleId));
            c.moveToNext();
        }
    }
}
