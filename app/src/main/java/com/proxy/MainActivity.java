package com.proxy;

import android.net.Uri;
import android.os.Build;
import android.content.Intent;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.tabs.TabLayout;
import com.proxy.listener.MessageListener;
import com.proxy.netty.Server;
import com.proxy.ui.Fragments.Intercept;
import com.proxy.ui.Fragments.Repeator;
import com.proxy.ui.Fragments.Target;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
	private static final String SELECTED_TAB_KEY = "selected_tab_key";
	/**
	 * Launcher for requesting storage permissions (Android 11+).
	 */
	private final ActivityResultLauncher<Intent> storagePermissionLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(), result -> {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
					Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
				}
			});

	/**
	 * Called when the activity is first created.
	 *
	 * @param savedInstanceState A Bundle containing the activity's previously saved state, if any.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initialize();
		askForPermissions();

		if (savedInstanceState != null) {
			int selectedTab = savedInstanceState.getInt(SELECTED_TAB_KEY, 0);
			ViewPager2 viewPager = findViewById(R.id.view_pager);
			viewPager.setCurrentItem(selectedTab, false);
		}

	}

	/**
	* Called when the activity is going to be destroyed.  Saves the current tab position.
	*
	* @param outState The Bundle in which to save the state.
	*/
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		ViewPager2 viewPager = findViewById(R.id.view_pager);
		outState.putInt(SELECTED_TAB_KEY, viewPager.getCurrentItem());
	}

	/**
	 * Requests the necessary storage permissions.  Handles Android 11+ permission request.
	 */
	public void askForPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
			intent.setData(Uri.parse("package:" + getPackageName()));
			storagePermissionLauncher.launch(intent);
		}
	}

	/**
	 * Initializes the UI components, including the TabLayout and ViewPager2.
	 */
	private void initialize() {
		TabLayout tabLayout = requireViewById(R.id.tab_layout);
		ViewPager2 viewPager = requireViewById(R.id.view_pager);
		ViewPagerAdapter adapter = new ViewPagerAdapter(this);

		adapter.addFragment(getString(R.string.target), new Target());
		adapter.addFragment(getString(R.string.interceptor), new Intercept());
		adapter.addFragment(getString(R.string.repeater), new Repeator());

		viewPager.setAdapter(adapter);
		new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			tab.setText(adapter.getTitle(position));
		}).attach();
	}

	/**
	 * Adapter for the ViewPager2. Manages the fragments displayed in the pager.
	 */
	public static class ViewPagerAdapter extends FragmentStateAdapter {
		private final ArrayList<String> titles = new ArrayList<>();
		private final ArrayList<Fragment> fragmentArrayList = new ArrayList<>();

		/**
		 * Constructor for the ViewPagerAdapter.
		 *
		 * @param fragmentActivity The FragmentActivity that hosts the ViewPager2.
		 */
		public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
			super(fragmentActivity);
		}

		/**
		 * Adds a fragment and its title to the adapter.
		 *
		 * @param name     The title of the fragment.
		 * @param fragment The fragment to add.
		 */
		public void addFragment(String name, Fragment fragment) {
			titles.add(name);
			fragmentArrayList.add(fragment);
		}

		/**
		 * Creates a new fragment at the specified position.
		 *
		 * @param position The position of the fragment to create.
		 * @return The newly created fragment.
		 */
		@NonNull
		@Override
		public Fragment createFragment(int position) {
			if (position < 0 || position >= fragmentArrayList.size()) {
				Log.e("ViewPagerAdapter", "Invalid position: " + position);
				return new Target(); // Replace with an actual default/fallback fragment
			}
			return fragmentArrayList.get(position);
		}

		/**
		 * Returns the total number of fragments in the adapter.
		 *
		 * @return The total number of fragments.
		 */
		@Override
		public int getItemCount() {
			return fragmentArrayList.size();
		}

		/**
		 * Returns the title of the fragment at the specified position.
		 *
		 * @param position The position of the title to return.
		 * @return The title of the fragment.
		 */
		public String getTitle(int position) {
			if (position < 0 || position >= titles.size()) {
				Log.e("ViewPagerAdapter", "Invalid position: " + position);
				return titles.get(0); // Return the first title as a fallback
			}
			return titles.get(position);
		}
	}
}