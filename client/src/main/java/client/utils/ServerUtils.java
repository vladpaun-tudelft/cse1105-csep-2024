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

	@Getter @Setter
	private static StompSession session;
	@Getter @Setter
	private StompSession.Subscription embeddedFilesSubscription;
	@Getter @Setter
	private StompSession.Subscription embeddedFilesDeleteUpdates;
	@Getter @Setter
	private StompSession.Subscription embeddedFilesRenameUpdates;
	@Getter @Setter
	private static List<Collection> unavailableCollections;

	@Inject
	public ServerUtils(Config config, DialogStyler dialogStyler) {
		this.config = config;
		this.dialogStyler = dialogStyler;
		collections = config.readFromFile();

		this.manager = LanguageManager.getInstance(this.config);
		this.bundle = this.manager.getBundle();
	}

	public void registerForEmbeddedFileUpdates(Note selectedNote, Consumer<Long> consumer) {
		if (embeddedFilesSubscription != null && session != null) {
			try {
				embeddedFilesSubscription.unsubscribe();
			} catch (IllegalStateException _) {}
		}

		String topic = "/topic/notes/" + selectedNote.getId() + "/files";
		embeddedFilesSubscription = session.subscribe(topic, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Long.TYPE;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				consumer.accept((Long) payload);
			}
		});
	}

	public void registerForEmbeddedFilesDeleteUpdates(Note selectedNote, Consumer<Long> consumer) {
		if (embeddedFilesDeleteUpdates != null && session != null) {
			try {
				embeddedFilesDeleteUpdates.unsubscribe();
			} catch (IllegalStateException _) {}
		}

		String topic = "/topic/notes/" + selectedNote.getId() + "/files/deleteFile";
		embeddedFilesDeleteUpdates = session.subscribe(topic, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Long.TYPE;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				consumer.accept((Long) payload);
			}
		});
	}

	public void registerForEmbeddedFilesRenameUpdates(Note selectedNote,
													  Consumer<Object[]> consumer) {
		if (embeddedFilesRenameUpdates != null && session != null) {
			try {
				embeddedFilesRenameUpdates.unsubscribe();
			} catch (IllegalStateException _) {}
		}

		String topic = "/topic/notes/" + selectedNote.getId() + "/files/renameFile";
		embeddedFilesRenameUpdates = session.subscribe(topic, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Object[].class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				consumer.accept(
						(Object[]) payload
				);
			}
		});
	}

	public void unregisterFromEmbeddedFileUpdates() {
		if (embeddedFilesSubscription != null && session != null) {
			try {
				embeddedFilesSubscription.unsubscribe();
			} catch (IllegalStateException _) {}
			embeddedFilesSubscription = null;
		}
		if (embeddedFilesDeleteUpdates != null && session != null) {
			try {
				embeddedFilesDeleteUpdates.unsubscribe();
			} catch (IllegalStateException _) {}
			embeddedFilesDeleteUpdates = null;
		}
		if (embeddedFilesRenameUpdates != null && session != null) {
			try {
				embeddedFilesRenameUpdates.unsubscribe();
			} catch (IllegalStateException _) {}
			embeddedFilesRenameUpdates = null;
		}
	}

	public void getWebSocketURL(String serverURL) {
		if (session != null) {
			session.disconnect();
		}

		String webSocket = serverURL.replace("http", "ws");
		webSocket += "websocket";
		session = connect(webSocket);
	}

	// for testing purposes
	public WebSocketStompClient getWebSocketStompClient(StandardWebSocketClient client) {
		return new WebSocketStompClient(client);
	}
	public StandardWebSocketClient getStandardWebSocketClient() {
		return new StandardWebSocketClient();
	}

	public StompSession connect(String url) {
		var client = getStandardWebSocketClient();
		var stomp = getWebSocketStompClient(client);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
		messageConverter.setObjectMapper(objectMapper);
		stomp.setMessageConverter(messageConverter);
		try {
            return stomp.connectAsync(url, new StompSessionHandlerAdapter() {}).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		throw new IllegalStateException();
	}

	public <T> void registerForMessages(String dest, Class<T> type, Consumer<T> consumer) {
		if (this.session == null) {
			return;
        }
		this.session.subscribe(dest, new StompFrameHandler() {
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

	public void send(String dest, Object o) {
		if (session == null || !session.isConnected()) {
			return;
		}
		session.send(dest, o);
	}

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
				String alertText = """
                        The notes of the following collections could not be retrieved because the \
                        server they are hosted on is unavailable.
                        You can try to get the notes in these collections later by refreshing.
                        
                        """;
				for (Collection collection : unavailableCollections) {
					alertText += collection.title + "\n";
				}
				dialogStyler.createStyledAlert(
						Alert.AlertType.INFORMATION,
						"Error",
						"Error",
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

	public EmbeddedFile getFileById(Note note, Long fileId) {
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

	public long getCollectionID(Collection collection) {
		if (!isServerAvailableWithAlert(collection.serverURL)) return -1;
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