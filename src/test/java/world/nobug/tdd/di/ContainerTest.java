package world.nobug.tdd.di;

import org.junit.jupiter.api.Nested;

public class ContainerTest {

    // 依赖选择相关的测试类
    @Nested
    public class DependenciesSelection{

        @Nested
        public class ProviderType {
        }

        @Nested
        public class Qualifier{
        }

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


