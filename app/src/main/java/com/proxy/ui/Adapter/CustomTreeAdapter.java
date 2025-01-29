package com.proxy.ui.Adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import com.proxy.R;

import com.proxy.data.HttpMessage;
import com.proxy.data.Message;

import com.proxy.ui.TreeNode;
import com.proxy.utils.Networks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * `CustomTreeAdapter` is a custom adapter for an ExpandableListView. It displays
 * a tree-like structure where groups represent hosts (or categories) and
 * children represent messages associated with those hosts. It uses a
 * `HashMap` to store the messages and an `ArrayList` to store the group titles.
 */
public class CustomTreeAdapter extends BaseExpandableListAdapter {

	private Context context;
	private final HashMap<String, List<Message>> expandableDetailList = new HashMap<>();
	private final List<String> expandableTitleList = new ArrayList<>();

	public CustomTreeAdapter(Context context) {
		this.context = context;

	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		String groupTitle = expandableTitleList.get(groupPosition);
		return expandableDetailList.get(groupTitle).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {

		String displayText = ((Message) getChild(groupPosition, childPosition)).displayText();
		if (view == null) {
			LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = infalInflater.inflate(R.layout.tree_item, null);
		}
		TextView childItem = (TextView) view.findViewById(R.id.tree_item_textview);
		childItem.setText(displayText);

		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		String groupTitle = expandableTitleList.get(groupPosition);

		return expandableDetailList.get(groupTitle).size();

	}

	@Override
	public Object getGroup(int groupPosition) {
		return expandableTitleList.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return expandableTitleList.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		if (groupPosition == expandableTitleList.size()) {
			groupPosition -= 1;

		}
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isLastChild, View view, ViewGroup parent) {

		String headerInfo = (String) getGroup(groupPosition);
		if (view == null) {
			LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inf.inflate(R.layout.tree_list, null);
		}

		TextView heading = (TextView) view.findViewById(R.id.tree_list_textview);
		heading.setText(headerInfo);

		return view;
	}



	

	/**
	 * Adds a new message, handling duplicates.
	 *
	 * @param message The message to add.
	 */
	public void addNewMessage(Message message) {
		if (message != null) {
			String genericHost = Networks.genericMultiCDNS(message.host());

			// Check if the host is already in expandableDetailList
			if (expandableDetailList.containsKey(genericHost)) {
				// Check if the message already exists in the list for this host
				List<Message> messages = expandableDetailList.get(genericHost);
				if (!messages.contains(message)) { // Avoid duplicate messages
					messages.add(message);

				}
			} else {
				// If the host is not in the list, add a new entry
				expandableTitleList.add(genericHost);
				List<Message> newMessages = new ArrayList<>();
				newMessages.add(message);
				expandableDetailList.put(genericHost, newMessages);

			}

			notifyDataSetChanged();
		}
	}

	/**
	 * Adds a log message as a group (host/category).
	 *
	 * @param logMessage The log message to add.
	 * used for debugging
	 */
	public void addLogMessage(String logMessage) {
		expandableTitleList.add(logMessage);
		notifyDataSetChanged();
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}