module coderodde.WikiGameKillerFXJava {
    requires java.desktop;
    requires java.logging;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires transitive coderodde.ThreadPoolBidirectionalBFSPathFinder;
    requires transitive coderodde.WikipediaGraphNodeExpanders;
    
    exports com.github.coderodde.wikipedia.game.killer.fx;
}
