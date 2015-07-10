package org.bbs.demo.apkparser;

import org.bbs.apkparser.ApkManifestParser;
import org.bbs.apkparser.PackageInfoX;
import org.bbs.apkparser.demo.R;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.Iterator;

public class MainActivity extends ActionBarActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		String apkFile = getApplicationInfo().publicSourceDir;
		PackageInfoX info = ApkManifestParser.parseAPk(this, apkFile, true, true);
		info.dump(PackageInfoX.DUMP_ALL);
		setContentView(createTreeView(info));
		
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

	View createTreeView(PackageInfoX info){
		TreeNode root = TreeNode.root();
		TreeNode manifestNode = new TreeNode("manifest");

		TreeNode useSdk = new TreeNode("mUsesSdk");
		useSdk.addChild(new TreeNode("mMaxSdkVersion: " + info.mUsesSdk.mMaxSdkVersion));
		useSdk.addChild(new TreeNode("mMinSdkVersion: " + info.mUsesSdk.mMinSdkVersion));
		useSdk.addChild(new TreeNode("mTargetSdkVersion: " + info.mUsesSdk.mTargetSdkVersion));
		manifestNode.addChild(useSdk);
		if (info.mUsedPermissions != null && info.mUsedPermissions.length > 0) {
			TreeNode useP = new TreeNode("mUsedPermissions");
			for (PackageInfoX.UsesPermissionX p : info.mUsedPermissions){
				TreeNode pN = new TreeNode(p.name);

				useP.addChild(pN);
			}
			manifestNode.addChild(useP);
		}

		TreeNode appNode = new TreeNode("applicationInfo");
		appNode.addChild(new TreeNode("name: " + info.applicationInfo.name));
		appNode.addChild(new TreeNode("className: " + info.applicationInfo.className));
		appNode.addChild(new TreeNode("packageName: " + info.applicationInfo.packageName));
		createMetaData(info.applicationInfo.metaData, appNode);
		manifestNode.addChild(appNode);

		if (info.activities != null && info.activities.length > 0){
			TreeNode actRoot = new TreeNode("activities");
			for (ActivityInfo a : info.activities){
				PackageInfoX.ActivityInfoX aX = (PackageInfoX.ActivityInfoX) a;
				TreeNode act = createActivityNode(aX);
				if (aX.mIntentFilters != null && aX.mIntentFilters.length > 0){
					TreeNode filtersN = new TreeNode("mIntentFilters");
					for (PackageInfoX.IntentFilterX f: aX.mIntentFilters){
						TreeNode filter = new TreeNode("intentfilter");
						int count = f.countActions();
						for  (int i = 0 ; i < count; i++){
							TreeNode aN = new TreeNode("action: " + f.getAction(i));
							filter.addChild(aN);
						}
						count = f.countCategories();
						for  (int i = 0 ; i < count; i++){
							TreeNode aN = new TreeNode("category: " + f.getCategory(i));
							filter.addChild(aN);
						}

						filtersN.addChild(filter);
					}

					act.addChild(filtersN);
				}
				createMetaData(aX.metaData, act);
				actRoot.addChild(act);
			}

			appNode.addChild(actRoot);
		}

		root.addChild(manifestNode);

		AndroidTreeView v = new AndroidTreeView(this, root);
		v.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
		return v.getView();
	}

	private void createMetaData(Bundle metaData, TreeNode root) {
		if (metaData != null){
			TreeNode nodes = new TreeNode("meta-datas");
			Iterator<String> it = metaData.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				String value = "";
				value = metaData.getString(key);
				if (null == value) {
					value = "@" + metaData.getInt(key) + "";
				}

				TreeNode node = new TreeNode("meta-data");
				node.addChildren(new TreeNode("key: " + key), new TreeNode("value: " + value));
				nodes.addChild(node);
			}
			root.addChild(nodes);
		}
	}

	private TreeNode createActivityNode(PackageInfoX.ActivityInfoX aX) {
		TreeNode act = new TreeNode(aX.name);
		TreeNode theme = new TreeNode("theme: " + aX.theme);
		TreeNode name = new TreeNode("name: " + aX.name);

		act.addChild(theme);
		act.addChild(name);

		return act;
	}


}
