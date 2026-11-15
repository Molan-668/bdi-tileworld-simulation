package multiagent_tileworld_plus;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.util.ContextUtils;

public class ModelController {

	@ScheduledMethod(start = 1, interval = 1)
    public void stopIfNoHoles() {
        Context<Object> context = ContextUtils.getContext(this);
        if (context.getObjects(Hole.class).size() == 0) {
            double finalTick = repast.simphony.engine.environment.RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
            System.out.println(">>> Simulation Finished! Total Ticks: " + finalTick);
            
            // 顺便打印所有 Agent 的总得分
            int totalScore = 0;
            for(Object obj : context.getObjects(Agent.class)) {
                totalScore += ((Agent)obj).getScore();
            }
            System.out.println(">>> Total Score: " + totalScore);
            
            RunEnvironment.getInstance().endRun();
        }
    }

}
