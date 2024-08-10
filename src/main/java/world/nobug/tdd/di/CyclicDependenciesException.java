package world.nobug.tdd.di;

public class CyclicDependenciesException extends RuntimeException{

    public Class<?>[] getComponents() {
        return new Class<?>[0];
    }
}
