package org.bbs.demo.apkparser;

import org.bbs.apkparser.ApkManifestParser;
import org.bbs.apkparser.PackageInfoX;
import org.bbs.apkparser.demo.R;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		String apkFile = getApplicationInfo().publicSourceDir;
		ApkManifestParser.parseAPk(this, apkFile, true, true).dump(PackageInfoX.DUMP_ALL);
		
		int flags = PackageManager.GET_ACTIVITIES|PackageManager.GET_CONFIGURATIONS
				|PackageManager.GET_DISABLED_COMPONENTS| PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
				|PackageManager.GET_GIDS| PackageManager.GET_INSTRUMENTATION 
				|PackageManager.GET_INTENT_FILTERS | PackageManager.GET_META_DATA
				|PackageManager.GET_PERMISSIONS | PackageManager.GET_PROVIDERS
				|PackageManager.GET_PROVIDERS | PackageManager.GET_RECEIVERS
				|PackageManager.GET_RESOLVED_FILTER | PackageManager.GET_SERVICES
				|PackageManager.GET_SHARED_LIBRARY_FILES| PackageManager.GET_SERVICES
				|PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_URI_PERMISSION_PATTERNS;
		PackageInfo pinfo = getPackageManager().getPackageArchiveInfo(apkFile, flags);
		Log.d(TAG, "pinfo: " + pinfo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
