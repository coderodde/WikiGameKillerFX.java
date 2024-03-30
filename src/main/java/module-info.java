module coderodde.WikiGameKillerFXJava {
    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.apache.commons.io;
    requires com.google.gson;
    requires transitive coderodde.ThreadPoolBidirectionalBFSPathFinder;
    requires transitive coderodde.WikipediaGraphNodeExpanders;
    
    exports com.github.coderodde.wikipedia.game.killer.fx;
}
