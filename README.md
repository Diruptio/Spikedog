Made in Germany <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/b/ba/Flag_of_Germany.svg/2560px-Flag_of_Germany.svg.png" width="20"> with love <font color="red">❤</font> and beer 🍺.
# Spikedog
Spikedog is a **lightweight** open source **HTTP server**, optimized for **web services** and good **performance**.
## Requirements
- Java 21 or higher
## Install
Folder Structure:
- directory
  - modules
    - info-module.jar
    - reload-module.jar
  - Spikedog.jar
  - start.sh
### Linux (bash)
1. Install Java 21 or higher (root required)
   ```bash
   sudo apt install openjdk-21-jdk
   ```
2. Install screen (root required)
   ```bash
   sudo apt install screen
   ```
3. Create an empty directory
   ```bash
   mkdir directory
   cd directory
   ```
4. Download [Spikedog.jar](https://github.com/Diruptio/Spikedog/releases/latest/download/Spikedog.jar)
   ```bash
   wget https://github.com/Diruptio/Spikedog/releases/latest/download/Spikedog.jar
   ```
5. Create start.sh with following content:
   ```bash
   screen -dmS spikedog java -jar ./Spikedog.jar
   ```
6. Make shart.sh executable
   ```bash
   chmod +x start
   ```
7. Start the server
   ```bash
   ./start.sh
   ```
   You can attach to the screen using `screen -r spikedog` and detach with `Ctrl + A + D`.
8. Autostart (optional)<br>
   Add the following line to ~/.profile:
   ```bash
   directory/start.sh
   ```
## Usage
### Modules
All modules are located in the `modules` directory. The server will load all modules on startup.<br>
If you wish to reload the modules at runtime, download [reload-module.jar](https://github.com/Diruptio/Spikedog/releases/latest/download/reload-module.jar) and place it in the `modules` directory.<br>
If you wish to see all loaded modules and sites/servlets at runtime, download [info-module.jar](https://github.com/Diruptio/Spikedog/releases/latest/download/info-module.jar) and place it in the `modules` directory.
### Module loading order
If you need to load a module before another module, you can create `order.txt` in your `modules` directory.
The files in `order.txt` will be loaded in the order they are listed and before the non-listed files. You also can use regular expressions in `order.txt`.
## Module Development
### Dependencies
Maven:
```xml
<repositories>
  <repository>
    <url>https://repo.diruptio.de/repository/maven-public/</url>
  </repository>
</repositories>
```
```xml
<dependencies>
  <dependency>
    <groupId>diruptio</groupId>
    <artifactId>Spikedog</artifactId>
    <version>VERSION</version>
  </dependency>
</dependencies>
```
Gradle:
```groovy
repositories {
    maven { url = "https://repo.diruptio.de/repository/maven-public/" }
}
```
```groovy
dependencies {
    implementation "diruptio:Spikedog:VERSION"
}
```
### Example Module
You can view the [example code](https://github.com/Diruptio/Spikedog/tree/main/example) or follow this tutorial
1. Create a Listener<br>
   Your Spikedog Listener must implement the `Listener` interface. It will be loaded automatically.
   ```java
   public class ExampleListener implements Listener {
       @Override
       public void onLoad(Module self) {
           System.out.println("Loading example module");
       }
   
       @Override
       public void onUnload() {
           System.out.println("Unloading example module");
       }
   }
   ```
2. Create a Servlet
   ```java
   public class ExampleServlet {
       @Endpoint(path = "/example")
       public void handle(HttpRequest request, HttpResponse response) {
           response.content("Hello World!");
       }
   }
   ```
3. Register your Servlet in the Listener
   ```java
   @Override
   public void onLoad(Module self) {
       System.out.println("Loading example module");
       
       Spikedog.register(new ExampleServlet());
   }
   ```
