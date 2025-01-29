package com.proxy.ui.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import com.proxy.AppContext;
import com.proxy.InitContext;
import com.proxy.R;
import android.view.View;
import android.view.LayoutInflater;
import com.proxy.ViewModel.SharedViewModel;
import com.proxy.beautifier.Beautifier;
import com.proxy.beautifier.HtmlBeautifier;
import com.proxy.beautifier.XmlBeautifier;
import com.proxy.beautifier.FormEncodedBeautifier;
import com.proxy.data.Http2Message;
import com.proxy.data.HttpHeaders;
import com.proxy.data.HttpMessage;
import com.proxy.data.HttpRequestHeaders;
import com.proxy.data.Message;
import com.proxy.listener.RepeaterListener;
import com.proxy.listener.SetLogger;
import com.proxy.netty.Server;
import androidx.fragment.app.Fragment;
import com.proxy.netty.codec.frame.Http2StreamEvent;
import com.proxy.store.Body;
import com.proxy.ui.Adapter.CustomTreeAdapter;
import com.proxy.utils.Networks;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import com.proxy.utils.BodyType;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.bouncycastle.util.encoders.UTF8;

public class Target extends Fragment {
	ExpandableListView expandableListView;
	CustomTreeAdapter adapter;
	FrameLayout frameLayoutTop, frameLayoutBottom;
	HttpMessage httpMessage;
	EditText httpTextView;
	Spinner spinner;
	ImageView show_full, more, cancel;
	SharedViewModel sharedViewModel;
	int currentType = 0;
	private final Set<String> processedMessages = new HashSet<>(); // Track unique hosts or IDs
	PopupMenu cachedPopupMenu;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private static final List<Beautifier> beautifiers = List.of(

			new FormEncodedBeautifier(), new XmlBeautifier(), new HtmlBeautifier());

	public Target() {
	}

	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
		View view = arg0.inflate(R.layout.target, arg1, false);
		initialize(view);

		//	sharedViewModel = new ViewModelProvider(getActivity()).get(SharedViewModel.class);
		AppContext context = new AppContext(requireContext());
		clickListeners();
		setupSpinner();
		start(context);
		InitContext initContext = new InitContext(requireContext());
		initContext.init();
		showMessagesToUi();

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
	}

	/**
	* Starts the server in a background thread.
	*
	* This method submits a task to the `executorService` to start the `Server`.  The `Server` is initialized
	* with settings from the `AppContext`, including server settings, SSL context manager, proxy settings, and
	* a message listener that calls `AddToViewModel()` when a new message arrives.  Any exceptions during
	* server startup are caught but currently ignored.
	*
	* @param appContext The application context providing necessary settings.
	*/
	private void start(AppContext appContext) {
		executorService.submit(() -> {
			Server server = new Server(appContext.getServerSetting(), appContext.getSslContextManager(),
					appContext.getProxySetting(), message -> {
						AddToViewModel(message);
					});
			try {
				server.start();
			} catch (Exception e) {

			}
		});
	}

	/**
	* Adds a new `Message` to the shared view model.  This operation is non-blocking.
	*
	* @param message The `Message` object to add.
	*/
	private void AddToViewModel(Message message) {
		sharedViewModel.addToMainRequests(message); // Non-blocking
	}

	/**
	* Observes the list of messages in the shared view model and updates the UI.
	*
	* This method observes the `mainRequests` LiveData in the `sharedViewModel`. When a new list of messages
	* is emitted, it creates a copy of the list to avoid potential ConcurrentModificationExceptions. Then,
	* it posts a Runnable to the main thread to update the adapter with the new messages.  This ensures
	* that UI updates are performed on the main thread.
	*/

	private void showMessagesToUi() {
		sharedViewModel.getMainRequests().observe(getViewLifecycleOwner(), messages -> {
			List<Message> safeMessages = new ArrayList<>(messages); // Create a copy

			getActivity().runOnUiThread(() -> {
				for (Message message : safeMessages) {
					adapter.addNewMessage(message);
				}
			});

		});

	}

	/**
	* Sets the formatted HTTP message (request or response) to the `httpTextView`.
	*
	* This method constructs the HTTP message string based on the provided `HttpMessage` and `type`.
	* It handles both request (type 0) and response (type 1) messages.  For requests, it combines
	* the header lines and body. For responses, it does the same, handling cases where the response
	* might not have headers.  Finally, it sets the constructed string to the `httpTextView` and
	* applies highlighting and formatting.
	*
	* @param message The `HttpMessage` object containing the request or response data.
	* @param type    An integer indicating the message type: 0 for request, 1 for response.
	*/
	private void setHttpMessage(HttpMessage message, int type) {
		StringBuilder httpMessageBuilder = new StringBuilder();

		if (type == 0) { // Request
			HttpHeaders httpHeaders = message.requestHeader();
			String body = getBody(message.requestBody());

			httpMessageBuilder.append(String.join("\n", httpHeaders.rawLines())).append("\n").append(body);

		} else if (type == 1) { // Response
			HttpHeaders httpHeaders = message.responseHeader();
			if (httpHeaders != null) {
				String httpResponse = String.join("\n", httpHeaders.rawLines()) + "\n"
						+ getBody(message.responseBody());
				httpMessageBuilder.append(httpResponse);
			} else {
				httpMessageBuilder.append("empty response");
			}
		}

		// Set the full HTTP message to the TextView in one go
		httpTextView.setText(httpMessageBuilder.toString());

		// Highlight and format the request/response
		highlightAndFormatHttpRequest(httpTextView);
	}

	/**
	* Initializes the UI components and sets up the expandable list view.
	*
	* This method performs the initial setup of the view, including initializing individual views
	* using `initializeViews()` and configuring the expandable list view with `setupExpandableListView()`.
	*
	* @param view The root `View` of the layout.
	*/
	private void initialize(View view) {
		initializeViews(view);
		setupExpandableListView();
	}

	/**
	 * Initializes all UI components using findViewById.
	 */
	private void initializeViews(View view) {
		frameLayoutTop = view.findViewById(R.id.target_framelayout_top);
		frameLayoutBottom = view.findViewById(R.id.target_framelayout_bottom);
		expandableListView = view.findViewById(R.id.expandableListView);
		httpTextView = view.findViewById(R.id.target_httpmessage_view);
		spinner = view.findViewById(R.id.target_spinner);
		show_full = view.findViewById(R.id.show_full_screen);
		more = view.findViewById(R.id.target_more);
		cancel = view.findViewById(R.id.target_cancel);
	}

	/**
	 * Sets up the ExpandableListView with a custom adapter.
	 */
	private void setupExpandableListView() {
		adapter = new CustomTreeAdapter(getContext());
		expandableListView.setAdapter(adapter);
	}

	/**
	* Sets up all the click listeners for the UI elements in this component.
	* This includes listeners for the expandable list view, the log callback, and regular buttons.
	*/
	private void clickListeners() {
		setupExpandableListViewClickListener();
		setupLogCallBack();
		setupButtonListeners();
	}

	/**
	 * Sets up the click listener for ExpandableListView child items.
	 */
	private void setupExpandableListViewClickListener() {
		expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition,
					long id) {
				Object child = adapter.getChild(groupPosition, childPosition);
				if (child instanceof HttpMessage) {
					httpMessage = (HttpMessage) child;
					if (frameLayoutBottom.getVisibility() == View.GONE) {
						frameLayoutBottom.setVisibility(View.VISIBLE);
					}
					setHttpMessage(httpMessage, 0);
				}
				return false;
			}
		});
	}

	/**
	 * Sets up the log callback to update the adapter with new log messages.
	 */
	private void setupLogCallBack() {
		SetLogger.setLogCallBack(new SetLogger.LogCallBack() {
			@Override
			public void onLog(String str) {
				getActivity().runOnUiThread(() -> adapter.addLogMessage(str));
			}
		});
	}

	/**
	 * Sets up click listeners for buttons (show_full, more, cancel).
	 */
	private void setupButtonListeners() {
		show_full.setOnClickListener(v -> toggleFrameLayoutVisibility(frameLayoutTop));
		more.setOnClickListener(v -> ShowPopUpMenu());
		cancel.setOnClickListener(v -> {
			if (frameLayoutTop.getVisibility() == View.GONE) {
				frameLayoutTop.setVisibility(View.VISIBLE);
			}
			frameLayoutBottom.setVisibility(View.GONE);
		});
	}

	/**
	 * Toggles the visibility of a FrameLayout.
	 */
	private void toggleFrameLayoutVisibility(FrameLayout frameLayout) {
		if (frameLayout.getVisibility() == View.VISIBLE) {
			frameLayout.setVisibility(View.GONE);
		} else {
			frameLayout.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Displays a pop-up menu.
	 */

	private void ShowPopUpMenu() {

		if (cachedPopupMenu == null) {
			cachedPopupMenu = new PopupMenu(getActivity(), more);
			cachedPopupMenu.getMenuInflater().inflate(R.menu.menu, cachedPopupMenu.getMenu());
			cachedPopupMenu.setOnMenuItemClickListener(item -> {
				if (item.getItemId() == R.id.send_to_repeator) {
					sharedViewModel.addToRepeater(httpMessage);
				}
				return true;
			});
		}
		cachedPopupMenu.show();

	}

	private void setupSpinner() {
		// Initialize the Spinner with options
		initializeSpinnerOptions();

		// Set up item selection listener
		setupSpinnerListener();
	}

	/**
	 * Initializes the Spinner with options "Request" and "Response".
	 */
	private void initializeSpinnerOptions() {
		String[] options = getResources().getStringArray(R.array.spinner_options);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_text, // Custom layout for spinner items
				options);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_item); // Default dropdown layout
		spinner.setAdapter(adapter);
	}

	/**
	 * Sets up the item selection listener for the Spinner.
	 */
	private void setupSpinnerListener() {
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				handleSpinnerSelection(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing
			}
		});
	}

	/**
	 * Handles the logic when a Spinner item is selected.
	 */
	private void handleSpinnerSelection(int selectedPosition) {
		if (httpMessage != null && currentType != selectedPosition) {
			currentType = selectedPosition;
			setHttpMessage(httpMessage, currentType);
		}
	}

	/**
	* Extracts the body content from a `Body` object, attempting to decode it as text and applying beautification.
	*
	* This method prioritizes reading the body as a text stream using the provided charset. If that fails or the
	* body is not of a text type, it falls back to converting the content to a string using UTF-8.  Finally, it applies
	* any applicable beautifiers to the extracted text.
	*
	* @param body The `Body` object from which to extract the content.
	* @return The extracted and beautified body content as a String, or an empty string if no content could be extracted.
	*/
	private String getBody(Body body) {
		String text = "";
		BodyType bodyType = body.type();
		Charset charset = body.charset().orElse(CharsetUtil.UTF_8);
		if (bodyType.isText()) {

			try (InputStream input = body.getDecodedInputStream();
					Reader reader = new InputStreamReader(input, charset)) {
				text = Body.readAll(reader);
			} catch (IOException e) {

			}
		}
		if (text.equals("")) {
			text = body.content().toString(StandardCharsets.UTF_8);
		}
		for (Beautifier beautifier : beautifiers) {
			if (beautifier.accept(bodyType)) {
				text = beautifier.beautify(text, charset);
				break;
			}
		}
		return text;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		executorService.shutdown();
	}

	/**
	 * Display the Request with formatting and text highlighting
	 */

	private void highlightAndFormatHttpRequest(EditText requestEditText) {

		String formattedText = formatHttpRequestText(requestEditText.getText().toString());

		SpannableStringBuilder highlightedText = highlightHttpRequestText(formattedText);

		requestEditText.setText(highlightedText);
	}

	/**
	 * Formats the HTTP request text by ensuring consistent spacing around colons in headers.
	 */
	private String formatHttpRequestText(String originalText) {
		if (originalText.isEmpty()) {
			return originalText; // Skip processing if the text is empty
		}

		StringBuilder formattedText = new StringBuilder();
		String[] lines = originalText.split("\n");

		for (String line : lines) {
			if (line.contains(":")) {
				int colonIndex = getColonIndex(line);
				String headerName = line.substring(0, colonIndex).trim();
				String headerValue = line.substring(colonIndex + 1).trim();

				// Append formatted line
				formattedText.append(headerName).append(": ").append(headerValue).append("\n");
			} else {
				// Keep non-header lines unchanged
				formattedText.append(line).append("\n");
			}
		}

		return formattedText.toString().trim();
	}

	/**
	 * Returns the index of the colon to use for splitting header names and values.
	 * Handles cases where multiple colons exist in a line.
	 */
	private int getColonIndex(String line) {
		int colonIndex = line.indexOf(":");
		return colonIndex != -1 && line.indexOf(":", colonIndex + 1) != -1 ? line.indexOf(":", colonIndex + 1)
				: colonIndex;
	}

	/**
	 * Applies syntax highlighting to the formatted HTTP request text.
	 */
	private SpannableStringBuilder highlightHttpRequestText(String formattedText) {
		SpannableStringBuilder stringBuilder = new SpannableStringBuilder(formattedText);
		String[] lines = formattedText.split("\n");

		int start = 0; // Track the cumulative position in the text
		for (String line : lines) {
			if (line.contains(":")) {
				int colonIndex = getColonIndex(line);

				// Highlight the header name (before the colon)
				applyHeaderHighlighting(stringBuilder, start, start + colonIndex, line.trim().startsWith(":"));
			}
			// Move the start position to the next line
			start += line.length() + 1; // +1 for the newline character
		}

		return stringBuilder;
	}

	/**
	 * Applies highlighting to the header name in the SpannableStringBuilder.
	 */
	private void applyHeaderHighlighting(SpannableStringBuilder stringBuilder, int start, int end,
			boolean isPseudoHeader) {
		int color = isPseudoHeader ? Color.parseColor("#FF0000") // Red for pseudo-headers
				: Color.parseColor("#efb92c"); // Yellow for regular headers

		stringBuilder.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}