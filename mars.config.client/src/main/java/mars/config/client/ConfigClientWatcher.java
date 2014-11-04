package mars.config.client;

public interface ConfigClientWatcher {

    public void process(ConfigEvent event);
}
