 
You can run the project out-of-the-box from your terminal via

	./gradlew run (macOS)
	.\gradle.bat run (Windows)

Running it within your IDE (Eclipse/IntelliJ) requires setting up OpenJFX.

First download (and unzip!) an [OpenJFX SDK](https://openjfx.io).
Make sure that the download *matches your Java JDK version*.

Then create a *run configuration* and add the following *VM* commands:

	--module-path="/path/to/javafx-sdk/lib"
	--add-modules=javafx.controls,javafx.fxml,javafx.web

Adjust the module path to your local download and make sure you adapt the path
to the `lib`(!) directory (not just the directory that you unzipped)...

*Tip:* Windows uses `\` to separate path elements.

*Tip:* Make sure not to forget the `/lib` at the end of the path

*Tip:* Double-check that the path is correct. If you receive abstract error messages, like `Module javafx.web not found`
or a segmentation fault, you are likely not pointing to the right folder
