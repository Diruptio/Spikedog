package diruptio.spikedog.spikedev;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class Spikedev implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        /*Property<Integer> port = new DefaultProperty<>(PropertyHost.NO_OP, Integer.class);
        port.set(8080);
        Property<String> bindAddress = new DefaultProperty<>(PropertyHost.NO_OP, String.class);
        bindAddress.set("0.0.0.0");
        Property<Boolean> useSsl = new DefaultProperty<>(PropertyHost.NO_OP, Boolean.class);
        useSsl.set(true);
        Property<Boolean> loadModules = new DefaultProperty<>(PropertyHost.NO_OP, Boolean.class);
        loadModules.set(true);
        Task task = */ target.getTasks().register("runSpikedog", RunSpikedogTask.class, target);
        /*task.getExtensions().add("port", port);
        task.getExtensions().add("bindAddress", bindAddress);
        task.getExtensions().add("useSsl", useSsl);
        task.getExtensions().add("loadModules", loadModules);
        TaskProvider<Jar> jar = target.getTasks().named("jar", Jar.class);
        task.doLast(t -> {
            Spikedog.MODULES_DIRECTORY = target.file("run/modules").toPath();
            Spikedog.ADDITIONAL_MODULES = new HashSet<>();
            for (File file : jar.get().getOutputs().getFiles()) {
                Spikedog.ADDITIONAL_MODULES.add(file.toPath());
            }
            Spikedog.listen(port.get(), bindAddress.get(), useSsl.get(), loadModules.get());
        });*/
    }
}
