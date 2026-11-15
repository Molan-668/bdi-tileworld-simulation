package multiagent_tileworld_plus;

import java.util.ArrayList;
import java.util.List;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;


public class Agent {
    private Grid<Object> grid;
    private Network<Object> network;
    
    private int id;
    private int sensingRange;
    private int score;
    private int battery; // 能量管理 

    private Tile targetTile;
    private RepastEdge<Object> targetTileEdge;
    private boolean holdingTile;
    
    private Hole targetHole;
    private RepastEdge<Object> targetHoleEdge;
    
    private int timeWithHoleButNoTile;
    private int releaseAfter;
    private boolean mustFindTileFirst;
    
    private GridPoint lastPos;
    private int escapeCounter = 0;

    public Agent(Grid<Object> grid, Network<Object> network, int sensingRange, int id, int releaseAfter) {
        this.grid = grid;
        this.network = network;
        this.sensingRange = sensingRange;
        this.score = 0;
        this.id = id;
        this.battery = 100; // 初始电量 
        this.releaseAfter = releaseAfter;
        this.resetTargets();
    }
    
    public int getScore() { return this.score; }
    public boolean hasTile() { return this.holdingTile; }
    public int getBattery() { return this.battery; }
    
    private void resetTargets() {
        this.targetTile = null;
        this.targetTileEdge = null;
        this.holdingTile = false;
        this.targetHole = null;
        this.targetHoleEdge = null;
        this.mustFindTileFirst = false;
    }
    
    @ScheduledMethod(start = 1, interval = 1)
    public void run() {        
        GridPoint curPos = this.grid.getLocation(this);
        Context<Object> context = ContextUtils.getContext(this);
        
        // === 欲望 1: 生存 (能量管理) ===
        // 如果电量不足以完成几次移动，优先寻找能量站充电 [cite: 57]
        if (this.battery <= 30) {
            EnergyStation nearestStation = findNearestEnergyStation(curPos, context);
            if (nearestStation != null) {
                GridPoint stationPos = this.grid.getLocation(nearestStation);
                if (curPos.equals(stationPos)) {
                    this.battery = 100; // 充电完成 [cite: 76]
                } else {
                    this.moveTowards(stationPos.getX(), stationPos.getY());
                }
                return; // 充电期间暂停其他意图
            }
        }

        // === 信念更新 & 协作机制 ===
        // 将自己感知到的未被认领的资源分享到黑板 
        List<GridCell<Tile>> sensedTiles = this.getCellsWithObjsAround(Tile.class);
        for(GridCell<Tile> cell : sensedTiles) {
            for(Tile t : cell.items()) { if(!t.isClaimed()) Blackboard.knownTiles.add(t); }
        }
        List<GridCell<Hole>> sensedHoles = this.getCellsWithObjsAround(Hole.class);
        for(GridCell<Hole> cell : sensedHoles) {
            for(Hole h : cell.items()) { if(!h.isClaimed()) Blackboard.knownHoles.add(h); }
        }

        // === 欲望 2: 获取瓷砖 ===
        if (this.targetTile == null && !this.holdingTile) {
            Tile selectedTile = this.selectTile(sensedTiles); // 优先看周围
            if (selectedTile == null) {
                // 如果周围没有，从协作黑板中寻找 
                selectedTile = getTileFromBlackboard(); 
            }
            if (selectedTile != null && !selectedTile.isClaimed()) {
                this.targetTile = selectedTile;
                this.targetTileEdge = this.network.addEdge(this, this.targetTile);
                selectedTile.claim();
                Blackboard.knownTiles.remove(selectedTile);
            }
        }
        
        // === 欲望 3: 填补坑洞 ===
        if (this.targetHole == null && (!this.mustFindTileFirst || this.targetTile != null || this.holdingTile)) {
            Hole selectedHole = this.selectHole(sensedHoles); // 获取最高分坑洞 
            if (selectedHole == null) {
                selectedHole = getHoleFromBlackboard();
            }
            if (selectedHole != null && !selectedHole.isClaimed()) {
                this.targetHole = selectedHole;
                this.targetHoleEdge = this.network.addEdge(this, this.targetHole);
                selectedHole.claim();
                Blackboard.knownHoles.remove(selectedHole);
                this.timeWithHoleButNoTile = 0;
            }
        } else {
            if (this.targetTile == null && !this.holdingTile) {
                this.timeWithHoleButNoTile ++;
                if (this.timeWithHoleButNoTile == this.releaseAfter) {
                    this.targetHole.release();
                    this.network.removeEdge(this.targetHoleEdge);
                    this.targetHole = null;
                    this.mustFindTileFirst = true;
                }
            }
        }

        // === 意图执行 (移动与交互) ===
        
        if (!this.holdingTile && this.targetTile != null) {
            GridPoint targetTilePos = this.grid.getLocation(this.targetTile);
            if (targetTilePos == null) {
                // 瓷砖可能被动态环境移走了，重置
                this.targetTile.claim(); // 撤销认领 (假定有 unclaim 方法，这里为简便直接设为空)
                this.targetTile = null; return;
            }
            if (!curPos.equals(targetTilePos)) {
                this.moveTowards(targetTilePos.getX(), targetTilePos.getY());
                return;
            }
            this.network.removeEdge(this.targetTileEdge);
            context.remove(this.targetTile);
            this.targetTile = null;
            this.holdingTile = true;
            return;
        }
        
        if (this.holdingTile && this.targetHole != null) {
            GridPoint targetHolePos = this.grid.getLocation(this.targetHole);
            if (!curPos.equals(targetHolePos)) {
                this.moveTowards(targetHolePos.getX(), targetHolePos.getY());
                return;
            }
            this.score += this.targetHole.getScore();
            this.network.removeEdge(this.targetHoleEdge);
            context.remove(this.targetHole);
            this.resetTargets();
            return;
        }
        
        this.stroll();
    }
    
    // --- 能量与协作辅助方法 ---
    
    private EnergyStation findNearestEnergyStation(GridPoint curPos, Context<Object> context) {
        EnergyStation nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Object obj : context.getObjects(EnergyStation.class)) {
            GridPoint sp = grid.getLocation(obj);
            double dist = Math.abs(curPos.getX() - sp.getX()) + Math.abs(curPos.getY() - sp.getY());
            if (dist < minDist) { minDist = dist; nearest = (EnergyStation) obj; }
        }
        return nearest;
    }
    
    private Tile getTileFromBlackboard() {
        for (Tile t : Blackboard.knownTiles) {
            if (!t.isClaimed() && grid.getLocation(t) != null) return t;
        }
        return null;
    }

    private Hole getHoleFromBlackboard() {
        Hole best = null;
        for (Hole h : Blackboard.knownHoles) {
            if (!h.isClaimed() && grid.getLocation(h) != null) {
                // 在黑板中也执行得分最大化策略 
                if (best == null || h.getScore() > best.getScore()) best = h;
            }
        }
        return best;
    }

    // --- 移动与避障策略  ---
    
    private void stroll() {
        // 没目标的时候，直接用随机游走代替以前死板的向右走
        moveRandomly();
    }
    
    private void moveTowards(int x, int y) {
        // 如果正在逃脱模式中，暂时忽略目标，继续随机游走绕远一点
        if (this.escapeCounter > 0) {
            this.escapeCounter--;
            moveRandomly();
            return;
        }

        GridPoint curPos = this.grid.getLocation(this);
        int curX = curPos.getX();
        int curY = curPos.getY();
        
        int nextX = curX, nextY = curY;
        if (curX != x) nextX = curX < x ? curX + 1 : curX - 1;
        else if (curY != y) nextY = curY < y ? curY + 1 : curY - 1;

        if (canMoveTo(nextX, nextY)) {
            performMove(nextX, nextY);
        } else {
            // 贪心寻路撞墙了！触发逃脱模式，强制连续随机走 3 步来彻底脱离死角
            this.escapeCounter = 3; 
            moveRandomly();
        }
    }

    private void moveRandomly() {
        GridPoint curPos = this.grid.getLocation(this);
        java.util.List<GridPoint> safeMoves = new java.util.ArrayList<>();
        int w = this.grid.getDimensions().getWidth();
        int h = this.grid.getDimensions().getHeight();
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}}; // 上下左右
        
        for(int[] d : dirs) {
            int nx = curPos.getX() + d[0];
            int ny = curPos.getY() + d[1];
            if(nx < 0) nx += w; if(nx >= w) nx -= w;
            if(ny < 0) ny += h; if(ny >= h) ny -= h;
            
            if(canMoveTo(nx, ny)) {
                // 不走刚走过的回头路
                if(lastPos == null || nx != lastPos.getX() || ny != lastPos.getY()) {
                    safeMoves.add(new GridPoint(nx, ny));
                }
            }
        }
        
        if(!safeMoves.isEmpty()) {
            int randIdx = repast.simphony.random.RandomHelper.nextIntFromTo(0, safeMoves.size() - 1);
            GridPoint chosen = safeMoves.get(randIdx);
            performMove(chosen.getX(), chosen.getY());
        } else {
            // 彻底被死胡同卡住，只能原路退回
            if (lastPos != null && canMoveTo(lastPos.getX(), lastPos.getY())) {
                performMove(lastPos.getX(), lastPos.getY());
            }
        }
    }

    private void performMove(int x, int y) {
        this.lastPos = this.grid.getLocation(this); 
        this.grid.moveTo(this, x, y);
        this.battery -= 1; 
    }
    
    private boolean canMoveTo(int x, int y) {
        int w = this.grid.getDimensions().getWidth();
        int h = this.grid.getDimensions().getHeight();
        if(x < 0) x += w; if(x >= w) x -= w;
        if(y < 0) y += h; if(y >= h) y -= h;

        Iterable<Object> objects = this.grid.getObjectsAt(x, y);
        for (Object obj : objects) {
            if (obj instanceof Obstacle) return false;
        }
        return true;
    }
    
    // --- 目标选择策略 ---
    
    private Tile selectTile(List<GridCell<Tile>> cellsWithTiles) {
        for (GridCell<Tile> cell : cellsWithTiles) {
            for (Tile t : cell.items()) { if (!t.isClaimed()) return t; }
        }
        return null;
    }
    
    private Hole selectHole(List<GridCell<Hole>> cellsWithHoles) {
        Hole bestHole = null;
        for (GridCell<Hole> cell : cellsWithHoles) {
            for (Hole h : cell.items()) {
                // 得分最大化策略：比较奖励值 
                if (!h.isClaimed() && (bestHole == null || h.getScore() > bestHole.getScore())) {
                    bestHole = h;
                }
            }
        }
        return bestHole;
      
        /*
    	for (GridCell<Hole> cell : cellsWithHoles) {
            for (Hole h : cell.items()) {
                // 只要没被认领，就直接选它！不管分数多低！
                if (!h.isClaimed()) {
                    return h; 
                }
            }
        }
        return null;
        */
    }
    
    //降级代码
    /*private Hole getHoleFromBlackboard() {
        for (Hole h : Blackboard.knownHoles) {
            // 黑板上的也一样，拿到第一个没被认领的就去，不比较分数
            if (!h.isClaimed() && grid.getLocation(h) != null) {
                return h; 
            }
        }
        return null;
    }*/

    private <T> List<GridCell<T>> getCellsWithObjsAround(Class<T> type) {
        GridPoint curPos = this.grid.getLocation(this);
        GridCellNgh<T> nghCreator = new GridCellNgh<T>(grid, curPos, type, this.sensingRange, this.sensingRange);
        return nghCreator.getNeighborhood(false);
    }
}