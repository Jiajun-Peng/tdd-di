package world.nobug.tdd.di;

interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
