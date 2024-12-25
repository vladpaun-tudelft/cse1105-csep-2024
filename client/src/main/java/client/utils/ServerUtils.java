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

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import org.glassfish.jersey.client.ClientConfig;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import static jakarta.ws.rs.core.MediaType.*;

public class ServerUtils {

	private static final String SERVER = "http://localhost:8080/";


	public Note addNote(Note note) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("api/notes")
				.request(APPLICATION_JSON)
				.post(Entity.entity(note, APPLICATION_JSON), Note.class);
	}

	public List<Note> getAllNotes() {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("api/notes")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Note>>() {});
	}

	public Note getNoteById(long id) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("api/notes/" + id)
				.request(APPLICATION_JSON)
				.get(Note.class);
	}

	public Note updateNote(Note note) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("api/notes/" + note.id)
				.request(APPLICATION_JSON)
				.put(Entity.entity(note, APPLICATION_JSON), Note.class);
	}

	public void deleteNote(long id) {
		ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("api/notes/" + id)
				.request(APPLICATION_JSON)
				.delete();
	}

	public List<Note> getNotesByCollection(Collection collection) {
		if (collection == null || collection.title == null || collection.title.isEmpty()) {
			throw new IllegalArgumentException("Collection or collection title cannot be null or empty");
		}

		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER)
				.path("/api/collection/{title}")
				.resolveTemplate("title", collection.title)
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Note>>() {});
	}

	public Collection addCollection(Collection collection) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection")
				.request(APPLICATION_JSON)
				.post(Entity.entity(collection, APPLICATION_JSON), Collection.class);
	}

	public List<Collection> getCollections() {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<Collection>>() {});
	}


	public Collection getCollectionByTitle(String title) {
		if (title == null || title.isEmpty()) {
			throw new IllegalArgumentException("Title cannot be null or empty");
		}

		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER)
				.path("/api/collection/title/{title}")
				.resolveTemplate("title", title)
				.request(APPLICATION_JSON)
				.get(Collection.class);
	}


	public Collection updateCollection(Collection collection) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection/" + collection.id)
				.request(APPLICATION_JSON)
				.put(Entity.entity(collection, APPLICATION_JSON), Collection.class);
	}

	public void deleteCollection(long id) {
		ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/collection/" + id)
				.request(APPLICATION_JSON)
				.delete();
	}

	public EmbeddedFile addFile(Note note, File file) throws IOException {
		// Convert file to MultipartFile
		FormDataMultiPart multiPart = new FormDataMultiPart();
		multiPart.bodyPart(new FileDataBodyPart("file", file));

		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/notes/" + note.id + "/files")
				.request(APPLICATION_JSON)
				.post(Entity.entity(multiPart, MULTIPART_FORM_DATA_TYPE), EmbeddedFile.class);
	}

	public void deleteFile(Note note, EmbeddedFile file) {
		ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("api/notes/" + note.id + "/files/" + file.getId())
				.request(APPLICATION_JSON)
				.delete();
	}

	public EmbeddedFile renameFile(Note note, EmbeddedFile file, String newFileName) {
		return ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("api/notes/" + note.id + "/files/" + file.getId() + "/rename")
				.queryParam("newFileName", newFileName)
				.request(APPLICATION_JSON)
				.put(Entity.entity(file, APPLICATION_JSON), EmbeddedFile.class);
	}

	public List<EmbeddedFile> getFilesByNote(Note note) {
		List<EmbeddedFile> result = ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/notes/" + note.id + "/files")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<EmbeddedFile>>() {});
		if (result == null)
			result = new ArrayList<>();
		return result;
	}

	public List<EmbeddedFile> getFilesByNote(Note note) {
		List<EmbeddedFile> result = ClientBuilder.newClient(new ClientConfig())
				.target(SERVER).path("/api/notes/" + note.id + "/files")
				.request(APPLICATION_JSON)
				.get(new GenericType<List<EmbeddedFile>>() {});
		if (result == null)
			result = new ArrayList<>();
		return result;
	}

	public boolean isServerAvailable() {
		try {
			ClientBuilder.newClient(new ClientConfig()) //
					.target(SERVER) //
					.request(APPLICATION_JSON) //
					.get();
		} catch (ProcessingException e) {
			if (e.getCause() instanceof ConnectException) {
				return false;
			}
		}
		return true;
	}
}