package multiagent_tileworld_plus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class Blackboard {
    // 线程安全的集合，用于存储全局已知的瓷砖和坑洞
    public static Set<Tile> knownTiles = ConcurrentHashMap.newKeySet();
    public static Set<Hole> knownHoles = ConcurrentHashMap.newKeySet();
    
    public static void clear() {
        knownTiles.clear();
        knownHoles.clear();
    }
}