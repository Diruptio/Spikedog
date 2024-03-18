Made in Germany <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/b/ba/Flag_of_Germany.svg/2560px-Flag_of_Germany.svg.png" width="20"> with love <font color="red">❤</font> and beer 🍺.
# Spikedog
Spikedog is a lightweight open source server for HTTP services.
## Install
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