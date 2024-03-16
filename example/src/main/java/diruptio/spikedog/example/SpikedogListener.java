package diruptio.spikedog.example;

import diruptio.spikedog.Jar;
import diruptio.spikedog.Listen;
import diruptio.spikedog.Listener;

public class SpikedogListener implements Listener {
    @Listen
    @Override
    public void onLoad(Jar self) {
        System.out.println("Loaded " + self.file());
    }

    @Listen
    @Override
    public void onUnload(Jar self) {
        System.out.println("Unloaded " + self.file());
    }
}
