package com.olumns.olumninet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.teamolumn.olumninet.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by chris on 10/27/13.
 */
public class MainActivity extends Activity {
    public String fullName, username, password;
    public DBHandler db = new DBHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        //Group Fragment
        GroupFragment listsFragment = new GroupFragment();
        EventsFragment eventsFragment = new EventsFragment();
        ProfileFragment profileFragment = new ProfileFragment();

        //Lists Fragment
        ActionBar.Tab listsTab = actionBar.newTab().setText(R.string.tab1);
        listsTab.setTabListener(new NavTabListener(listsFragment));

        //Events Fragment
        ActionBar.Tab eventsTab = actionBar.newTab().setText(R.string.tab2);
        eventsTab.setTabListener(new NavTabListener(eventsFragment));

        //Profile Fragment
        ActionBar.Tab profileTab = actionBar.newTab().setText(R.string.tab3);
        profileTab.setTabListener(new NavTabListener(profileFragment));

        //Adding the different fragments
        actionBar.addTab(listsTab);
        actionBar.addTab(eventsTab);
        actionBar.addTab(profileTab);

        //Action Bar
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.android_dark_blue)));

        //OnFirstRun
        onFirstRun();

        //Synchronize with Server


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //Olin Network Credentials Authentication
    public void authenticate(){
        new AsyncTask<Void, Void, String>() {
            HttpResponse response;
            InputStream inputStream = null;
            String result = "";
            HttpClient client = new DefaultHttpClient();

            @Override
            protected void onPreExecute(){
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
            }
            protected String doInBackground(Void... voids) {
                //Website URL and header configuration
                String website = "https://olinapps.herokuapp.com/api/exchangelogin";
                HttpPost get_auth = new HttpPost(website);
                get_auth.setHeader("Content-type","application/json");

                //Create and execute POST with JSON Post Package
                JSONObject auth = new JSONObject();
                try{
                    auth.put("username", MainActivity.this.username);
                    auth.put("password", MainActivity.this.password);
                    StringEntity se = new StringEntity(auth.toString());
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,"application/json"));
                    get_auth.setEntity(se);
                }catch(Exception e){e.printStackTrace();}
                try{response = client.execute(get_auth);}catch(Exception e){e.printStackTrace();}

                //Read the response
                HttpEntity entity = response.getEntity();

                try{inputStream = entity.getContent();}catch(Exception e){e.printStackTrace();}
                try{BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),8);
                    StringBuilder sb = new StringBuilder(); String line; String nl = System.getProperty("line.separator");

                    while ((line = reader.readLine())!= null){
                        sb.append(line);
                        sb.append(nl);
                    }
                    result = sb.toString();}catch(Exception e){e.printStackTrace();}

                //Convert Result to JSON
                String username = "";
                try{
                    auth = new JSONObject(result);
                    JSONObject userID = auth.getJSONObject("user");
                    username = userID.getString("id");
                }catch(Exception e){e.printStackTrace();}
                return username;
            }
            protected void onPostExecute(String fullName){
                MainActivity.this.fullName = fullName;
                //Save FullName
                getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                        .edit()
                        .putString("fullName", MainActivity.this.fullName)
                        .commit();
                Toast.makeText(MainActivity.this, "You have logged in as " + MainActivity.this.fullName, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    //Get Group Names
    public ArrayList<String> getGroupNames () {
        /*ArrayList<String>   (Arrays.asList(getSharedPreferences("PREFERENCE", MODE_PRIVATE).getString("notifications", "NULL").split("#,")));*/
    }

    //Do this on first run
    public void onFirstRun(){
        if (!usernameExists()) userLogin();
        if (!groupsExist()) addGroup();
    }

    //Get User Name
    public boolean usernameExists(){
        this.username = getSharedPreferences("PREFERENCE",MODE_PRIVATE).getString("username","");
        return !this.username.equals("");
    }

    //Dialog Log in
    public void userLogin(){
        //Inflate Dialog View
        final View view = MainActivity.this.getLayoutInflater().inflate(R.layout.signin_main,null);
        //Prompt for username and password
        new AlertDialog.Builder(MainActivity.this)
                .setView(view)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EditText userInput = (EditText) view.findViewById(R.id.username);
                        EditText passInput = (EditText) view.findViewById(R.id.password);
                        MainActivity.this.username = userInput.getText().toString();
                        MainActivity.this.password = passInput.getText().toString();
                        //Save to preference
                        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                .edit()
                                .putString("username", userInput.getText().toString())
                                .commit();

                        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                                .edit()
                                .putString("password", passInput.getText().toString())
                                .commit();
                        authenticate();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
        //Get User Login
        MainActivity.this.username = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getString("username","");
        MainActivity.this.password = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getString("password","");
    }

    //Add Group
    public void addGroup(){

    }

    public boolean groupsExist(){
        String groups = getSharedPreferences("PREFERENCE",MODE_PRIVATE).getString("groupsInfo","");
        return !groups.equals("");
    }

    public void updateLocalDatabase(){
        new AsyncTask<Void, Void, String>() {
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;


            @Override
            protected void onPreExecute() {
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
            }

            protected String doInBackground(Void... voids) {
                try {
                    String website = "http://olumni-server.heroku.com/" + fullName + "/getMissingPosts";
                    HttpPost createSessions = new HttpPost(website);

                    ArrayList<String> groupArray = getGroupNames();

                    String groupsString = makeStingFromArraylist(getGroupNames());
                    String postIDString = makeStingFromArraylist(db.getAllPostIds());

                    JSONObject json = new JSONObject();
                    json.put("postIDs", postIDString);
                    json.put("groups",groupsString);


                    StringEntity se = new StringEntity(json.toString());
                    Log.i("JSON ENTITY",se.toString());
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,"application/json"));
                    createSessions.setEntity(se);

                    response = client.execute(createSessions);
                }
                catch (Exception e) {e.printStackTrace(); Log.e("Server", "Cannot Establish Connection");}
                String result = "";
                try{
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"),8);
                    StringBuilder sb = new StringBuilder();

                    String line;
                    String nl = System.getProperty("line.separator");
                    while ((line = reader.readLine())!= null){
                        sb.append(line + nl);
                    }
                    result = sb.toString();
                    Log.i("RESULT PRINT FROM THING", result);
                }catch (Exception e){e.printStackTrace();}

                return result;
            }

            protected void onPostExecute(String result){
                DBHandler db = new DBHandler(getApplicationContext());
                db.open();

                if (result != null && !result.isEmpty()) {
                    if (!result.equals("")){
                        JSONArray jArray = new JSONArray();
                        // ArrayList tweets = new ArrayList();
                        JSONObject jsonObj = null;
                        try{
                            jsonObj = new JSONObject(result);
                        }catch (JSONException e){
                            Log.i("jsonParse", "error converting string to json object");
                        }
                        try {
                            jArray = jsonObj.getJSONArray("posts");
                        } catch(JSONException e) {
                            e.printStackTrace();
                            Log.i("jsonParse", "error converting to json array");
                        }
                        for (int i=0; i < jArray.length(); i++)
                            try {
                                JSONObject postObject = jArray.getJSONObject(i);
                                JSONArray viewerArray = postObject.getJSONArray("viewers");
                                StringBuilder viewerString = new StringBuilder();
                                for (int j=0; j < viewerArray.length(); j++) {
                                    viewerString.append(viewerArray.getString(j));
                                    if (viewerString.length() > 0 &&  j != viewerArray.length()-1) {
                                        viewerString.append("#");
                                    }

                                }

                                // Pulling items from the array
                                String group = postObject.getString("group");
                                String parent = postObject.getString("parent");
                                String userName = postObject.getString("username");
                                String date = postObject.getString("date");
                                String lastDate = postObject.getString("lastDate");
                                String message = postObject.getString("message");
                                String resolved = postObject.getString("resolved");
                                String reply = postObject.getString("reply");
                                String subject = postObject.getString("subject");
                                String id = postObject.getString("_id");
                                String viewers = viewerString.toString();
                                String poster, groups, subject, message, date, parent, status, id
                                Post post = new Post(userName, subject, message, );
                                db.addPost(post);

                            } catch (JSONException e) {
                                Log.i("jsonParse", "error in iterating");
                            }
                    }

                } else {Log.i("jsonParse", "result is null");}
            }
        }.execute();finish();
    }

    public String makeStingFromArraylist(ArrayList<String> array) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : array) {
            sb.append(s);
            System.out.println(i);
            System.out.println(array.size());
            if (sb.length() > 0 && i != array.size()-1) {
                sb.append("&");
                i++;
            }

        }
        return sb.toString();
    }
}
