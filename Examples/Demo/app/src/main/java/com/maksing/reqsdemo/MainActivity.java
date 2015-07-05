package com.maksing.reqsdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by maksing on 5/7/15.
 */
public class MainActivity extends Activity {

    private Class activities[] = {SimpleParallelRequestsActivity.class, SimpleSequenceRequestsActivity.class, AsyncRequestActivity.class, ComplexFlowActivity.class};
    private String titles[] = {"Simple parallel requests flow", "Simple sequential requests flow", "AsyncRequest and ForEachRequest Test", "ReqsRequest, SwitchRequest, retry and some random flow test..."};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView listView = new ListView(this);
        setContentView(listView);

        listView.setAdapter(new ArrayAdapter<>(this, R.layout.listitem, titles));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(new Intent(MainActivity.this, activities[position]));
            }
        });
    }
}
