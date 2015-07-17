package org.bbs.demo.apkparser;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.bbs.apkparser.demo.R;

public class AppPicker extends ActionBarActivity {

    private ArrayAdapter<PackageInfo> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);

        mAdapter = new ArrayAdapter<PackageInfo>(this, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = new TextView(AppPicker.this);
                PackageInfo info = getItem(position);
                t.setText(info.packageName);

                return t;
//                return super.getView(position, convertView, parent);
            }
        };
        mAdapter.addAll(getPackageManager().getInstalledPackages(0));

        ListView listV = new ListView(this);
        listV.setAdapter(mAdapter);
        setContentView(listV);
        listV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PackageInfo info = mAdapter.getItem(position);
                Intent data = new Intent();
                data.putExtra("app", info);
                setResult(RESULT_OK, data);

                finish();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_app_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    
}
