package com.maksing.reqsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import reqs.Reqs;

/**
 * Created by maksing on 4/7/15.
 */
public abstract class BaseActivity extends Activity {

    private TextView textLog;

    protected Reqs reqs = getReqs().setOnCancelListener(new Reqs.OnCancelListener() {
        @Override
        public void onCancel(Reqs reqs) {
            log("cancelled");
        }
    }).setOnPauseListener(new Reqs.OnPauseListener() {
        @Override
        public void onPause(Reqs reqs) {
            log("paused");
        }
    }).setOnResumeListener(new Reqs.OnResumeListener() {
        @Override
        public void onResume(Reqs reqs) {
            log("resume");
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textLog = (TextView)findViewById(R.id.requestsLog);

        TextView startBtn = findView(R.id.btnStart);
        TextView pauseBtn = findView(R.id.btnPause);
        TextView resumeBtn = findView(R.id.btnResume);
        TextView cancelBtn = findView(R.id.btnCancel);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textLog.setText("");
                if (reqs != null) {
                    reqs.cancel();
                }
                reqs = Reqs.createWithReqs(reqs);
                reqs.start();
            }
        });

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reqs.pause();
            }
        });

        resumeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reqs.resume();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reqs.cancel();
            }
        });

        reqs.start();
    }

    protected abstract Reqs getReqs();

    @SuppressWarnings("unchecked")
    private <E> E findView(int id) {
        return (E)super.findViewById(id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reqs.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        reqs.pause();
    }

    @Override
    protected void onDestroy() {
        reqs.cancel();
        super.onDestroy();
    }

    protected void log(String message) {
        textLog.setText(textLog.getText() + "\n" + message);
    }
}
