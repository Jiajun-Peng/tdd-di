package world.nobug.tdd.di;

public class DependencyNotFoundException extends RuntimeException{
    private Class<?> dependency;

    public DependencyNotFoundException(Class<?> dependency) {
        this.dependency = dependency;
    }

    public Class<?> getDependency() {
        return dependency;
    }
}
