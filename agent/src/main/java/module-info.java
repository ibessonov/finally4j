module com.github.ibessonov.finally4j.agent {
    requires java.instrument;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;

    exports com.github.ibessonov.finally4j.agent;
}