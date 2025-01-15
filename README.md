# CSEP Template Project

This repository contains the template for the CSE project. Please extend this README.md with sufficient instructions that will illustrate for your TA and the course staff how they can run your project.

To run the template project from the command line, you either need to have [Maven](https://maven.apache.org/install.html) installed on your local system (`mvn`) or you need to use the Maven wrapper (`mvnw`). You can then execute

	mvn clean install

to package and install the artifacts for the three subprojects. Afterwards, you can run ...

	cd server
	mvn spring-boot:run

to start the server or ...

	cd client
	mvn javafx:run

to run the client. Please note that the server needs to be running, before you can start the client.

Once this is working, you can try importing the project into your favorite IDE.

## Keyboard Shortcuts
#### Dashboard:
- ```CTRL + N``` or ```ALT + N``` -- New Note
- ```CTRL + SHIFT + N``` or ```ALT + SHIFT + N``` - New Collection


- ```CTRL + E``` or ``` ALT + E``` - Edit current note name
- ```CTRL + SHIFT + E``` or ```ALT + SHIFT + E``` - Edit collections


- ```ALT + DELETE``` - Delete current note


- ```ALT + DOWN ARROW``` - Show next note
- ```ALT + UP ARROW``` - Show previous note
- ```ALT + RIGHT ARROW``` - Show next collection
- ```ALT + LEFT ARROW``` - Show previous collection


- ```ESCAPE``` - Clear search and focus search field
- ```F5``` - Refresh
- ```F11``` or ```ALT + ENTER``` - Fullscreen

#### Collections Stage
- ```CTRL + N``` or ```ALT + N``` - Add new collection
- ```CTRL + S``` or ```ALT + S``` - Save collection
- ```ALT + C``` - Create/Connect collection
- ```ALT + DELETE``` - Delete collection
- ```ALT + SHIFT + DELETE``` - Forget collection


- ```UP ARROW``` or ```DOWN ARROW``` - Navigate through collections
- ```TAB``` or ```SHIFT + TAB``` - Navigate through input fields & buttons


- ```ESCAPE``` - Close Stage