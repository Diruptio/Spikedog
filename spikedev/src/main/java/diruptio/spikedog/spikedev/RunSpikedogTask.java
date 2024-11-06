package diruptio.spikedog.spikedev;

import diruptio.spikedog.Spikedog;
import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.internal.provider.PropertyHost;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

public class RunSpikedogTask extends DefaultTask {
    private @Input TaskProvider<Task> from;

    protected RunSpikedogTask(@NotNull Project target) {
        setGroup("spikedev");
        Property<Integer> port = new DefaultProperty<>(PropertyHost.NO_OP, Integer.class);
        port.set(8080);
        Property<String> bindAddress = new DefaultProperty<>(PropertyHost.NO_OP, String.class);
        bindAddress.set("0.0.0.0");
        Property<Boolean> useSsl = new DefaultProperty<>(PropertyHost.NO_OP, Boolean.class);
        useSsl.set(true);
        Property<Boolean> loadModules = new DefaultProperty<>(PropertyHost.NO_OP, Boolean.class);
        loadModules.set(true);
        getExtensions().add("port", port);
        getExtensions().add("bindAddress", bindAddress);
        getExtensions().add("useSsl", useSsl);
        getExtensions().add("loadModules", loadModules);
        doLast(t -> {
            Spikedog.MODULES_DIRECTORY = target.file("run/modules").toPath();
            Spikedog.ADDITIONAL_MODULES = new HashSet<>();
            for (File file : from.get().getOutputs().getFiles()) {
                Spikedog.ADDITIONAL_MODULES.add(file.toPath());
            }
            Spikedog.listen(port.get(), bindAddress.get(), useSsl.get(), loadModules.get());
        });
    }

    public @NotNull TaskProvider<Task> getFrom() {
        return from;
    }

    public void setFrom(@NotNull TaskProvider<Task> from) {
        this.from = Objects.requireNonNull(from);
    }
}
