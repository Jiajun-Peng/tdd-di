package world.nobug.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Nested;

public class ContainerTest {

    // 依赖选择相关的测试类
    @Nested
    public class DependenciesSelection{

    }

    // 生命周期管理相关的测试类
    @Nested
    public class LifecycleManagement{

    }

}


interface Component{
    default Dependency dependency() {return null;}
}

interface Dependency{
}

interface AnotherDependency{
}

class ComponentWithDefaultConstructor implements Component{
    public ComponentWithDefaultConstructor(){
    }
}

class ComponentWithInjectConstructor implements Component{
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency){
        this.dependency = dependency;
    }

    // 用于测试验证dependency是否被注入
    public Dependency dependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component{

    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value){
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name){
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {

    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}


