package world.nobug.tdd.di;

public class DependencyNotFoundException extends RuntimeException {
    private Class<?> dependency;
    private Class<?> component;
    private Component dependencyComponent;
    private Component componentComponent;

    public DependencyNotFoundException(Class<?> component, Class<?> dependency) {
        this.dependency = dependency;
        this.component = component;
    }

    public DependencyNotFoundException(Component componentComponent, Component dependencyComponent) {
        this.dependencyComponent = dependencyComponent;
        this.componentComponent = componentComponent;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }

    public Component getDependencyComponent() {
        return this.dependencyComponent;
    }

    public Component getComponentComponent() {
        return this.componentComponent;
    }
}
