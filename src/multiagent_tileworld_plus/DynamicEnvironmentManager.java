package multiagent_tileworld_plus;

import java.util.ArrayList;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridDimensions;
import repast.simphony.util.ContextUtils;

public class DynamicEnvironmentManager {
    private Grid<Object> grid;
    private Context<Object> context;
    private int updateInterval;
    
    public DynamicEnvironmentManager(Context<Object> context, Grid<Object> grid, int numObstacles, int updateInterval) {
        this.grid = grid;
        this.context = context;
        this.updateInterval = updateInterval;
        
        for (int i = 0; i < numObstacles; i++) {
            Obstacle obst = new Obstacle(i);
            this.context.add(obst);
            
            moveToRandomEmptySpot(obst, grid.getDimensions());
        }
    }
    
    // 由 Builder 手动注册到 Schedule 中
    public void updateDynamics() {
        GridDimensions dims = this.grid.getDimensions();
        
        // 1. 移动所有障碍物 [cite: 49]
        for (Object obj : context.getObjects(Obstacle.class)) {
            moveToRandomEmptySpot(obj, dims);
        }
        
        // 2. 移动所有未被持有的瓷砖 [cite: 49]
        for (Object obj : context.getObjects(Tile.class)) {
            Tile tile = (Tile) obj;
            // 如果瓷砖已经被认领或被拿着，就不移动它
            if (!tile.isClaimed()) {
                moveToRandomEmptySpot(tile, dims);
            }
        }
    }
    
    private void moveToRandomEmptySpot(Object obj, GridDimensions dims) {
        int x=0, y=0;
        for(int i=0; i<=1000; i++) {
            x = RandomHelper.nextIntFromTo(0, dims.getWidth() - 1);
            y = RandomHelper.nextIntFromTo(0, dims.getHeight() - 1);
            if (!this.grid.getObjectsAt(x, y).iterator().hasNext()) {
                break;
            }
        }
        this.grid.moveTo(obj, x, y);
    }
}