package multiagent_tileworld_plus;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.space.grid.SimpleGridAdder;

public class MultiagentTileworldPlusBuilder implements ContextBuilder<Object>{
    public static String gridName = "grid";
    public static String networkName = "network";
    
    @Override
    public Context build(Context<Object> context) {
        context.setId("multiagent_tileworld_plus");
        Blackboard.clear(); // 重置黑板
        
        Parameters params = RunEnvironment.getInstance().getParameters();
        int gridW = params.getInteger("grid_width");
        int gridH = params.getInteger("grid_height");
        int numHoles = params.getInteger("num_holes");
        int agentSensingRange = params.getInteger("agent_sensing_range");
        int numObstacles = params.getInteger("obstacles");
        int numAgents = params.getInteger("agents");
        int releaseAfter = params.getInteger("releaseAfter");
        
        // 新增参数：能量站数量和动态更新间隔 
        int numStations = params.getInteger("energy_stations"); 
        int updateInterval = params.getInteger("dynamic_update_interval"); 
        
        NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>(networkName, context, true);
        Network<Object> network = netBuilder.buildNetwork();
        
        GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
        Grid<Object> grid = gridFactory.createGrid(
        	    gridName, context,
        	    new GridBuilderParameters<Object>(new WrapAroundBorders(), new SimpleGridAdder<Object>(), true, gridW, gridH)
        	);
        
        // 添加坑洞和瓷砖
        for (int i = 0; i < numHoles; i++) {
            int score = RandomHelper.nextIntFromTo(1, 10);
            addToEmptySpot(new Hole(i, score), context, grid);
            addToEmptySpot(new Tile(i), context, grid);
        }

        // 添加能量站 
        for (int i = 0; i < numStations; i++) {
        	addToEmptySpot(new EnergyStation(i), context, grid);
        }

        // 添加智能体
        for (int i =0; i < numAgents; i++ ) {
        	addToEmptySpot(new Agent(grid, network, agentSensingRange, i, releaseAfter), context, grid);
        }
        
        // 添加动态环境管理器 [cite: 49]
        DynamicEnvironmentManager envManager = new DynamicEnvironmentManager(context, grid, numObstacles, updateInterval);
        
        // 使用 Repast 调度器按用户设定的间隔注册更新事件
        ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
        ScheduleParameters schedParams = ScheduleParameters.createRepeating(updateInterval, updateInterval);
        schedule.schedule(schedParams, envManager, "updateDynamics");
        
        context.add(new ModelController());
        return context;
    }
    
    private void addToEmptySpot(Object obj, Context<Object> context, Grid<Object> grid) {
        context.add(obj); // 先加进系统
        int w = grid.getDimensions().getWidth();
        int h = grid.getDimensions().getHeight();
        int x, y;
        do {
            x = RandomHelper.nextIntFromTo(0, w - 1);
            y = RandomHelper.nextIntFromTo(0, h - 1);
        } while (grid.getObjectsAt(x, y).iterator().hasNext()); // 如果这格有人，就重新随一个坐标
        grid.moveTo(obj, x, y); // 移过去
    }
}
