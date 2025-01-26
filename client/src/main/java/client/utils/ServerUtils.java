/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package client.utils;

import client.LanguageManager;
import client.ui.DialogStyler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Inject;
import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import javafx.scene.control.Alert;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.File;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;

public class ServerUtils {

	private final Config config;
	private List<Collection> collections;
	private DialogStyler dialogStyler;
	private LanguageManager manager;
	private ResourceBundle bundle;

//	@Getter @Setter
//	private static StompSession session;
	@Getter @Setter
	private StompSession.Subscription embeddedFilesSubscription;
	@Getter @Setter
	private StompSession.Subscription embeddedFilesDeleteUpdates;
	@Getter @Setter
	private StompSession.Subscription embeddedFilesRenameUpdates;

	@Getter @Setter
	private StompSession.Subscription noteBodySubscription;
	@Getter @Setter
	private StompSession.Subscription noteTitleSubscription;

	@Getter @Setter
	private static List<Collection> unavailableCollections;

	@Getter private static Map<String, StompSession> sessions = new HashMap<>();
	@Getter private static Map<String, Map<String, StompSession.Subscription>> sessionSubscriptions = new HashMap<>();



	@Inject
	public ServerUtils(Config config, DialogStyler dialogStyler) {
		this.config = config;
		this.dialogStyler = dialogStyler;
		collections = config.readFromFile();

		this.manager = LanguageManager.getInstance(this.config);
		this.bundle = this.manager.getBundle();
	}

	// ----------------------- SOCKET CONNECTION MANAGEMENT -----------------------

	/**
	 * Establish a WebSocket connection for the given server URL.
	 * If the session already exists, it will reuse it.
	 *
	 * @param serverURL The server URL for which to establish a connection.
	 */
	public void getWebSocketURL(String serverURL) {
		sessions.computeIfAbsent(serverURL, url -> {
			String webSocketURL = convertToWebSocketURL(url);
			return connect(webSocketURL);
		});
	}

	/**
	 * Convert an HTTP/HTTPS URL to its WebSocket equivalent.
	 *
	 * @param serverURL The server URL to convert.
	 * @return The WebSocket equivalent URL.
	 */
	private String convertToWebSocketURL(String serverURL) {
		return serverURL.replace("http", "ws") + "websocket";
	}

	/**
	 * Create and configure a WebSocketStompClient for testing or production.
	 *
	 * @param client The standard WebSocket client.
	 * @return A configured WebSocketStompClient instance.
	 */
	public WebSocketStompClient getWebSocketStompClient(StandardWebSocketClient client) {
		WebSocketStompClient stompClient = new WebSocketStompClient(client);
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
		messageConverter.setObjectMapper(objectMapper);

		stompClient.setMessageConverter(messageConverter);
		return stompClient;
	}

	/**
	 * Establish a WebSocket connection to the specified URL.
	 *
	 * @param url The WebSocket URL.
	 * @return A connected StompSession.
	 */
	public StompSession connect(String url) {
		try {
			WebSocketStompClient stompClient = getWebSocketStompClient(new StandardWebSocketClient());
			return stompClient.connectAsync(url, new StompSessionHandlerAdapter() {}).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Thread interrupted while connecting to WebSocket: " + url, e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Failed to connect to WebSocket: " + url, e);
		}
	}

	/**
	 * Disconnect from the WebSocket for the specified server URL, removing all subscriptions and the session.
	 *
	 * @param serverURL The server URL to disconnect.
	 */
	public void disconnect(String serverURL) {
		// Unsubscribe from all topics for this server
		Map<String, StompSession.Subscription> subscriptions = sessionSubscriptions.remove(serverURL);
		if (subscriptions != null) {
			subscriptions.values().forEach(subscription -> {
				try {
					subscription.unsubscribe();
				} catch (IllegalStateException ignored) {
					// Already unsubscribed or invalid state, safe to ignore
				}
			});
		}

		// Disconnect the session
		StompSession session = sessions.remove(serverURL);
		if (session != null) {
			session.disconnect();
		}
	}


	// ----------------------- MULTI SUBSCRIPTION LOGIC -----------------------


	private void addSubscription(String serverURL, String type, StompSession.Subscription subscription) {
		sessionSubscriptions
				.computeIfAbsent(serverURL, k -> new HashMap<>())
				.put(type, subscription);
	}

	private StompSession.Subscription getSubscription(String serverURL, String type) {
		return sessionSubscriptions.getOrDefault(serverURL, Collections.emptyMap()).get(type);
	}

	private void removeSubscription(String serverURL, String type) {
		Map<String, StompSession.Subscription> subscriptions = sessionSubscriptions.get(serverURL);
		if (subscriptions != null) {
			StompSession.Subscription subscription = subscriptions.remove(type);
			if (subscription != null) {
				try {
					subscription.unsubscribe();
				} catch (IllegalStateException ignored) {}
			}
			if (subscriptions.isEmpty()) {
				sessionSubscriptions.remove(serverURL);
			}
		}
	}

	// ----------------------- REGISTERING FOR SUBSCRIPTIONS -----------------------

	public <T> void registerForTopic(String serverURL, String topic, Class<T> payloadType, String subscriptionType, Consumer<T> consumer) {
		if (!isServerAvailable(serverURL)) {
			return;
		}

		getWebSocketURL(serverURL);

		// Unsubscribe from the previous subscription if it exists
		removeSubscription(serverURL, subscriptionType);

		// Subscribe to the topic and register the new subscription
		StompSession.Subscription subscription = sessions.get(serverURL).subscribe(topic, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return payloadType;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				consumer.accept(payloadType.cast(payload));
			}
		});

		// Add the subscription to the session-specific map
		addSubscription(serverURL, subscriptionType, subscription);
	}

	public void unregisterNoteSubscriptions(String serverURL) {
		removeSubscription(serverURL, "embeddedFiles");
		removeSubscription(serverURL, "embeddedFilesDelete");
		removeSubscription(serverURL, "embeddedFilesRename");
		removeSubscription(serverURL, "noteBody");
	}

	public <T> void registerForMessages(String dest, Class<T> type, Consumer<T> consumer, String url) {
		if (sessions.get(url) == null) {
			return;
		}
		this.sessions.get(url).subscribe(dest, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return type;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				consumer.accept((T) payload);
			}
		});
	}


	// ----------------------- SENDING SOCKET UPDATES -----------------------

	public void send(String dest, Object o, String url) {
		if (sessions.get(url) == null || !sessions.get(url).isConnected()) {
			return;
		}
		sessions.get(url).send(dest, o);
	}


	// ----------------------- NORMAL SERVER METHODS -----------------------

	public Note addNote(Note note) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes")
				.request(APPLICATION_JSON)
				.post(Entity.entity(note, APPLICATION_JSON), Note.class);
	}

	public List<Note> getAllNotes() {
		collections = config.readFromFile();
		List<Note> allNotes = new ArrayList<>();
		unavailableCollections = new ArrayList<>();
		if (collections != null) {
			for (Collection collection : collections) {
				if (!isServerAvailable(collection.serverURL)) {
					if (!unavailableCollections.contains(collection)) {
						unavailableCollections.add(collection);
					}
				}
				else {
					allNotes.addAll(getNotesByCollection(collection));
				}
			}
			if (!unavailableCollections.isEmpty()) {
				String alertText = bundle.getString("unavailableCollectionsError");
				for (Collection collection : unavailableCollections) {
					alertText += collection.title + "\n";
				}
				dialogStyler.createStyledAlert(
						Alert.AlertType.INFORMATION,
						bundle.getString("error.text"),
						bundle.getString("error.text"),
						alertText
				).showAndWait();
			}
		}
		return allNotes;
	}

	public Note updateNote(Note note) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id)
				.request(APPLICATION_JSON)
				.put(Entity.entity(note, APPLICATION_JSON), Note.class);
	}

	public void deleteNote(Note note) {
		if(!isServerAvailableWithAlert(note.collection.serverURL)) return;

		List<EmbeddedFile> embeddedFiles = getFilesByNote(note);
		for (EmbeddedFile e : embeddedFiles) {
			deleteFile(note, e);
		}
		ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id)
				.request(APPLICATION_JSON)
				.delete();
	}

	public List<Note> getNotesByCollection(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL)
				.path("/api/collection/{title}")
				.resolveTemplate("title", collection.title)
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Note>>() {});
	}

	public List<Collection> getCollectionsOnServer(String serverURL) {
		if (!isServerAvailableWithAlert(serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(serverURL).path("/api/collection/")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Collection>>() {});
	}

	public Collection addCollection(Collection collection) {
		if(!isServerAvailableWithAlert(collection.serverURL)) return null;
		if (!collection.serverURL.endsWith("/")) collection.serverURL = collection.serverURL + "/";
		return ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL).path("/api/collection")
				.request(APPLICATION_JSON)
				.post(Entity.entity(collection, APPLICATION_JSON), Collection.class);
	}


	public Collection updateCollection(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL).path("/api/collection/" + collection.id)
				.request(APPLICATION_JSON)
				.put(Entity.entity(collection, APPLICATION_JSON), Collection.class);
	}

	public void deleteCollection(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return;

		List<Note> notesInCollection = getNotesByCollection(collection);
		for (Note note : notesInCollection) {
			deleteNote(note);
		}

		ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL).path("/api/collection/" + collection.id)
				.request(APPLICATION_JSON)
				.delete();
	}

	public EmbeddedFile addFile(Note note, File file){
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		// Convert file to MultipartFile
		FormDataMultiPart multiPart = new FormDataMultiPart();
		multiPart.bodyPart(new FileDataBodyPart("file", file));

		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("/api/notes/" + note.id + "/files")
				.request(APPLICATION_JSON)
				.post(Entity.entity(multiPart, MULTIPART_FORM_DATA_TYPE), EmbeddedFile.class);
	}

	public void deleteFile(Note note, EmbeddedFile file) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return;
		ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id + "/files/" + file.getId())
				.request(APPLICATION_JSON)
				.delete();
	}

	public EmbeddedFile renameFile(Note note, EmbeddedFile file, String newFileName) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id + "/files/" + file.getId() + "/rename")
				.queryParam("newFileName", newFileName)
				.request(APPLICATION_JSON)
				.put(Entity.entity(file, APPLICATION_JSON), EmbeddedFile.class);
	}

	public EmbeddedFile getFileById(Note note, UUID fileId) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		return ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("api/notes/" + note.id + "/files/" + fileId + "/getFile")
				.request(APPLICATION_JSON)
				.get(new GenericType<EmbeddedFile>() {});
	}

	public List<EmbeddedFile> getFilesByNote(Note note) {
		if (!isServerAvailableWithAlert(note.collection.serverURL)) return null;
		List<EmbeddedFile> result = ClientBuilder.newClient(new ClientConfig())
				.target(note.collection.serverURL).path("/api/notes/" + note.id + "/files")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<EmbeddedFile>>() {});
		if (result == null)
			result = new ArrayList<>();
		return result;
	}

	public UUID getCollectionID(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return null;
		Collection fetchedCollection = ClientBuilder.newClient(new ClientConfig())
				.target(collection.serverURL).path("/api/collection/title/" + collection.title)
				.request(APPLICATION_JSON)
				.get(Collection.class);
		return fetchedCollection.id;
	}

	public boolean isServerAvailableWithAlert(String serverUrl) {
		if (!isServerAvailable(serverUrl)) {
			dialogStyler.createStyledAlert(
					Alert.AlertType.ERROR,
					bundle.getString("serverUnreachable.text"),
					bundle.getString("serverCouldNotBeReached.text"),
					bundle.getString("unreachable.text")
			).showAndWait();

			return false;
		}
		return true;
	}

	public boolean isServerAvailable(String serverUrl) {
		if (!isValidUrl(serverUrl)) return false;
		try {
			if (serverUrl == null);
			ClientBuilder.newClient(new ClientConfig()) //
					.target(serverUrl) //
					.request(APPLICATION_JSON) //
					.get();
			return true;
		} catch (ProcessingException e) {
			if (e.getCause() instanceof ConnectException) {
				return false;
			}
		}
		return false;
	}

	public boolean isValidUrl(String url) {
		try {
			URI uri = new URI(url);

			// Ensure the scheme is either http or https
			String scheme = uri.getScheme();
			if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
				return false;
			}

			// Ensure there is a valid host
			String host = uri.getHost();
			if (host == null || host.isEmpty()) {
				return false;
			}

			// Ensure the URL is not only scheme and host (e.g., "http://")
			if (uri.getPath() == null && uri.getQuery() == null && uri.getFragment() == null) {
				return true; // Acceptable for cases like "http://test.com"
			}

			return true; // URL is valid
		} catch (URISyntaxException e) {
			return false; // Invalid URL syntax
		}
	}

}