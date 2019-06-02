import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;

public class App {

    private final String znodeName = "/z";
    private ProgramWatcher programWatcher;
    private ChildrenWatcher childrenWatcher;
    private ZooKeeper zooKeeper;
    private String processName;
    private Process process;

    public App(String processName) throws IOException {
        this.processName = processName;

        zooKeeper = new ZooKeeper("localhost:2181", 10000, null);
        programWatcher = new ProgramWatcher();
        childrenWatcher = new ChildrenWatcher();
    }

    private void start() throws KeeperException, InterruptedException {
        zooKeeper.exists(znodeName, programWatcher);
        zooKeeper.exists(znodeName, childrenWatcher);
    }

    private int childrenNumber(String path) throws KeeperException, InterruptedException {
        List<String> children = this.zooKeeper.getChildren(path, false);

        int amount = 0;
        for (String child : children) {
            String childrenPath = path + "/" + child;
            amount += childrenNumber(childrenPath);
        }

        amount += children.size();

        return amount;
    }

    private void startProgram() throws IOException {
        Runtime runTime = Runtime.getRuntime();
        process = runTime.exec(processName);
    }

    private void stopProgram() {
        process.destroy();
    }

    private class ProgramWatcher implements Watcher {

        @Override
        public void process(WatchedEvent watchedEvent) {
            switch (watchedEvent.getType()) {
                case NodeCreated:
                    try {
                        App.this.zooKeeper.getChildren(App.this.znodeName, App.this.childrenWatcher);
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        App.this.startProgram();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case NodeDeleted:
                    App.this.stopProgram();
                    break;
            }

            try {
                App.this.zooKeeper.exists(App.this.znodeName, this);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private class ChildrenWatcher implements Watcher {

        @Override
        public void process(WatchedEvent watchedEvent) {
            switch (watchedEvent.getType()) {
                case NodeChildrenChanged:
                    try {
                        System.out.println("Number of children: " + App.this.childrenNumber(App.this.znodeName));
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        App.this.zooKeeper.getChildren(App.this.znodeName, this);
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }

    }

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        App app = new App(args[0]);

        app.start();

        while (true) {
        }
    }

}
