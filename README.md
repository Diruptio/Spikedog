Made in Germany <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/b/ba/Flag_of_Germany.svg/2560px-Flag_of_Germany.svg.png" width="20"> with love <font color="red">❤</font> and beer 🍺.
# Spikedog
Spikedog is a **lightweight** and good **performant** open source **HTTP server** optimized for **Web APIs**.<br>
Every **HTTP service / Servlet** is deployed from a **Module**.
## Install
Folder Structure:
- modules
  - example-module.jar
  - reload-module.jar
- Spikedog.jar
- startscript
### Linux (bash)
1. Install Java 17 or higher
2. Download the [latest release](https://github.com/Diruptio/Spikedog/releases/latest/download/Spikedog.jar)
3. Create the start script 
   ```bash
   echo "java -jar ./Spikedog.jar &" > path/to/your/startscript
   chmod +x start
   ```
4. Start the server
   ```bash
   bash path/to/your/startscript
   ```
5. Create auto start script
   ```bash
   sudo echo "cd path/to/your && bash ./startscript" > /etc/init.d/spikedog
   sudo chmod +x /etc/init.d/spikedog
   sudo chown root:root /etc/init.d/spikedog
   ```
## Module Development
### Dependencies
Maven:
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```
```xml
<dependencies>
  <dependency>
    <groupId>com.github.Diruptio</groupId>
    <artifactId>Spikedog</artifactId>
    <version>VERSION</version>
  </dependency>
</dependencies>
```
Gradle:
```groovy
repositories {
    maven { url = "https://jitpack.io" }
}
```
```groovy
dependencies {
    implementation 'com.github.Diruptio:Spikedog:VERSION'
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
   public class ExampleServlet implements BiConsumer<HttpRequest, HttpResponse> {
       public void accept(HttpRequest request, HttpResponse response) {
           response.setStatus(200, "OK");
           response.setHeader("Content-Type", "text/plain");
           response.setContent("Hello World!");
       }
   }
   ```
3. Register your Servlet in the Listener
   ```java
   @Override
   public void onLoad(Module self) {
       System.out.println("Loading example module");
       
       Spikedog.addServlet("/example", new ExampleServlet());
   }
   ```