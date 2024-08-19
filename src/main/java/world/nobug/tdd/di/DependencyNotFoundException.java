package world.nobug.tdd.di;

public class DependencyNotFoundException extends RuntimeException {
    private Component dependency;
    private Component component;

    public DependencyNotFoundException(Component component, Component dependency) {
        this.dependency = dependency;
        this.component = component;
    }

    public Component getDependency() {
        return this.dependency;
    }

    public Component getComponent() {
        return this.component;
    }
}
