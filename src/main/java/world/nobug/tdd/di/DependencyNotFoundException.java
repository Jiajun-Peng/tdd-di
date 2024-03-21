package world.nobug.tdd.di;

public class DependencyNotFoundException extends RuntimeException{
    private Class<?> dependency;
    private Class<?> component;

    public DependencyNotFoundException(Class<?> component, Class<?> dependency) {
        this.component = component;
        this.dependency = dependency;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }
}
